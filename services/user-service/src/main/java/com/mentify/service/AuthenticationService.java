package com.mentify.service;

import com.mentify.config.KeycloakProperties;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.LoginResponse;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.AccountAccessDeniedException;
import com.mentify.exception.AuthenticationFailedException;
import com.mentify.exception.KeycloakAuthenticationException;
import com.mentify.repository.UserRepository;
import com.mentify.service.authentication.AuthenticationEventLogger;
import com.mentify.service.authentication.KeycloakAuthenticationClient;
import com.mentify.service.authentication.KeycloakAuthenticationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String ACCOUNT_NOT_ALLOWED = "Account is not allowed to access the platform";
    private static final String ACCOUNT_LOCKED = "Your account is locked";

    private final KeycloakAuthenticationClient keycloakAuthenticationClient;
    private final UserRepository userRepository;
    private final KeycloakProperties keycloakProperties;
    private final AuthenticationEventLogger authenticationEventLogger;

    @Transactional(noRollbackFor = {
            AuthenticationFailedException.class,
            AccountAccessDeniedException.class
    })
    public LoginResponse login(LoginRequest request) {
        try {
            KeycloakAuthenticationResult keycloakSession = keycloakAuthenticationClient.authenticate(request);
            User user = loadLocalProfile(keycloakSession);
            validateRoleConsistency(user, keycloakSession.getRealmRoles());
            activateInvitedProfile(user);
            validateLocalAccountStatus(user);

            user.resetLoginAttempts();
            user.setLastLogin(LocalDateTime.now());

            LoginResponse response = buildLoginResponse(keycloakSession, user);
            authenticationEventLogger.loginSucceeded(user.getId(), user.getKeycloakUserId(), user.getRole());
            return response;
        } catch (AuthenticationFailedException exception) {
            if (recordFailedLoginAttempt(request)) {
                throw new AccountAccessDeniedException(ACCOUNT_LOCKED);
            }
            authenticationEventLogger.loginFailed("INVALID_CREDENTIALS", safeIdentifier(request), null);
            throw exception;
        } catch (KeycloakAuthenticationException exception) {
            authenticationEventLogger.loginFailed("KEYCLOAK_UNAVAILABLE", safeIdentifier(request), null);
            throw exception;
        }
    }

    private User loadLocalProfile(KeycloakAuthenticationResult keycloakSession) {
        Optional<User> userByKeycloakId = Optional.empty();
        if (keycloakSession.getKeycloakUserId() != null && !keycloakSession.getKeycloakUserId().isBlank()) {
            userByKeycloakId = userRepository.findByKeycloakUserId(keycloakSession.getKeycloakUserId());
        }

        Optional<User> localUser = userByKeycloakId
                .or(() -> findByEmail(keycloakSession.getEmail()))
                .or(() -> findByEmailIgnoreCase(keycloakSession.getEmail()));

        if (localUser.isEmpty()) {
            authenticationEventLogger.loginFailed(
                    "LOCAL_PROFILE_MISSING",
                    safeEmail(keycloakSession.getEmail()),
                    keycloakSession.getKeycloakUserId()
            );
            throw new AccountAccessDeniedException("Local user profile is not available");
        }

        return localUser.get();
    }

    private Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim());
    }

    private Optional<User> findByEmailIgnoreCase(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    private boolean recordFailedLoginAttempt(LoginRequest request) {
        String identifier = safeIdentifier(request);
        if (identifier == null) {
            return false;
        }

        Optional<User> localUser = findByEmail(identifier)
                .or(() -> findByEmailIgnoreCase(identifier));

        if (localUser.isEmpty()) {
            return false;
        }

        User user = localUser.get();
        if (!user.isAccountNonLocked()) {
            authenticationEventLogger.loginFailed(
                    "LOCAL_ACCOUNT_LOCKED",
                    safeEmail(user.getEmail()),
                    user.getKeycloakUserId()
            );
            return true;
        }

        user.incrementLoginAttempts();
        if (!user.isAccountNonLocked()) {
            authenticationEventLogger.loginFailed(
                    "LOCAL_ACCOUNT_LOCKED",
                    safeEmail(user.getEmail()),
                    user.getKeycloakUserId()
            );
            return true;
        }

        return false;
    }

    private void validateLocalAccountStatus(User user) {
        if (user.isFullyActive()) {
            return;
        }

        if (!user.isAccountNonLocked()) {
            authenticationEventLogger.loginFailed(
                    "LOCAL_ACCOUNT_LOCKED",
                    safeEmail(user.getEmail()),
                    user.getKeycloakUserId()
            );
            throw new AccountAccessDeniedException(ACCOUNT_LOCKED);
        }

        authenticationEventLogger.loginFailed(
                "LOCAL_ACCOUNT_NOT_ACTIVE",
                safeEmail(user.getEmail()),
                user.getKeycloakUserId()
        );
        throw new AccountAccessDeniedException(ACCOUNT_NOT_ALLOWED);
    }

    private void activateInvitedProfile(User user) {
        if (user.getAccountStatus() != AccountStatus.INVITED) {
            return;
        }

        if (!user.isAccountNonLocked()) {
            authenticationEventLogger.loginFailed(
                    "LOCAL_ACCOUNT_LOCKED",
                    safeEmail(user.getEmail()),
                    user.getKeycloakUserId()
            );
            throw new AccountAccessDeniedException(ACCOUNT_LOCKED);
        }

        if (!user.isActive() || !isProfileComplete(user)) {
            authenticationEventLogger.loginFailed(
                    "INVITED_PROFILE_NOT_ACTIVATED",
                    safeEmail(user.getEmail()),
                    user.getKeycloakUserId()
            );
            throw new AccountAccessDeniedException(ACCOUNT_NOT_ALLOWED);
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
    }

    private void validateRoleConsistency(User user, Set<String> keycloakRoles) {
        String localRole = normalizeRoleName(user.getRole().name());
        if (keycloakRoles != null && keycloakRoles.stream().map(this::normalizeRoleName).anyMatch(localRole::equals)) {
            return;
        }

        authenticationEventLogger.loginFailed(
                isPrivileged(user.getRole()) ? "PRIVILEGED_ROLE_MISMATCH" : "ROLE_MISMATCH",
                safeEmail(user.getEmail()),
                user.getKeycloakUserId()
        );
        throw new AccountAccessDeniedException("Account role is not consistent");
    }

    private boolean isPrivileged(Role role) {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    private LoginResponse buildLoginResponse(KeycloakAuthenticationResult keycloakSession, User user) {
        return LoginResponse.builder()
                .accessToken(keycloakSession.getAccessToken())
                .refreshToken(keycloakProperties.isIncludeRefreshTokenInLoginResponse() ? keycloakSession.getRefreshToken() : null)
                .tokenType(keycloakSession.getTokenType())
                .expiresIn(keycloakSession.getExpiresIn())
                .refreshExpiresIn(keycloakProperties.isIncludeRefreshTokenInLoginResponse()
                        ? keycloakSession.getRefreshExpiresIn()
                        : null)
                .scope(keycloakSession.getScope())
                .issuedAt(Instant.now())
                .user(LoginResponse.UserSummary.builder()
                        .id(user.getId())
                        .keycloakUserId(user.getKeycloakUserId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .accountStatus(user.getAccountStatus())
                        .emailVerified(user.isEmailVerified())
                        .profileComplete(isProfileComplete(user))
                        .build())
                .build();
    }

    private boolean isProfileComplete(User user) {
        if (user.getRole() == Role.STUDENT) {
            return user.getStudentProfile() != null;
        }
        if (user.getRole() == Role.TEACHER) {
            return user.getTeacherProfile() != null;
        }
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN) {
            return user.getAdminProfile() != null;
        }
        return false;
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if ("SUPERADMIN".equals(normalized)) {
            return Role.SUPER_ADMIN.name();
        }
        return normalized;
    }

    private String safeIdentifier(LoginRequest request) {
        if (request == null || request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            return null;
        }
        return request.getIdentifier().trim().toLowerCase(Locale.ROOT);
    }

    private String safeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
