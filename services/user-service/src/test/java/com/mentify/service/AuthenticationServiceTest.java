package com.mentify.service;

import com.mentify.config.KeycloakProperties;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.LoginResponse;
import com.mentify.entity.AdminProfile;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private KeycloakAuthenticationClient keycloakAuthenticationClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationEventLogger authenticationEventLogger;

    private KeycloakProperties keycloakProperties;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        keycloakProperties = new KeycloakProperties();
        keycloakProperties.setIncludeRefreshTokenInLoginResponse(true);
        authenticationService = new AuthenticationService(
                keycloakAuthenticationClient,
                userRepository,
                keycloakProperties,
                authenticationEventLogger
        );
    }

    @Test
    void login_whenSuccessful_loadsLocalProfileAndReturnsSafeMetadata() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.ACTIVE);
        user.setStudentProfile(new StudentProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        LoginResponse response = authenticationService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("student@gmail.com");
        assertThat(response.getUser().getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.getUser().isProfileComplete()).isTrue();
        assertThat(user.getLoginAttempts()).isZero();
        assertThat(user.getLastLogin()).isNotNull();

        verify(authenticationEventLogger).loginSucceeded(user.getId(), "keycloak-student-id", Role.STUDENT);
    }

    @Test
    void login_whenRefreshTokensAreDisabled_omitsRefreshTokenMetadata() {
        keycloakProperties.setIncludeRefreshTokenInLoginResponse(false);
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.ACTIVE);
        user.setStudentProfile(new StudentProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        LoginResponse response = authenticationService.login(request);

        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getRefreshExpiresIn()).isNull();
    }

    @Test
    void login_whenInvalidCredentials_doesNotLookupLocalProfileAndLogsGenericFailure() {
        LoginRequest request = loginRequest();
        when(keycloakAuthenticationClient.authenticate(request))
                .thenThrow(new AuthenticationFailedException("Invalid email or password"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(userRepository);
        verify(authenticationEventLogger).loginFailed("INVALID_CREDENTIALS", "student@gmail.com", null);
    }

    @Test
    void login_whenKeycloakUnavailable_mapsToControlledServiceFailure() {
        LoginRequest request = loginRequest();
        when(keycloakAuthenticationClient.authenticate(request))
                .thenThrow(new KeycloakAuthenticationException("Authentication service is unavailable"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(KeycloakAuthenticationException.class)
                .hasMessage("Authentication service is unavailable");

        verifyNoInteractions(userRepository);
        verify(authenticationEventLogger).loginFailed("KEYCLOAK_UNAVAILABLE", "student@gmail.com", null);
    }

    @Test
    void login_whenLocalProfileMissing_rejectsAccessAndLogsInconsistency() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-user-id", "student@gmail.com", Set.of("STUDENT"));

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-user-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("student@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountAccessDeniedException.class)
                .hasMessage("Local user profile is not available");

        verify(authenticationEventLogger).loginFailed("LOCAL_PROFILE_MISSING", "student@gmail.com", "keycloak-user-id");
    }

    @Test
    void login_whenLocalProfileIsInvitedAndComplete_activatesProfileAndReturnsSuccess() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.INVITED);
        user.setStudentProfile(new StudentProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        LoginResponse response = authenticationService.login(request);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.getUser().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(authenticationEventLogger).loginSucceeded(user.getId(), "keycloak-student-id", Role.STUDENT);
    }

    @Test
    void login_whenLocalProfileIsInvitedButIncomplete_rejectsAccess() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.INVITED);

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountAccessDeniedException.class)
                .hasMessage("Account is not allowed to access the platform");

        verify(authenticationEventLogger).loginFailed("INVITED_PROFILE_NOT_ACTIVATED", "student@gmail.com", "keycloak-student-id");
    }

    @Test
    void login_whenLocalProfileIsSuspended_rejectsAccess() {
        assertInactiveStatusIsRejected(AccountStatus.SUSPENDED);
    }

    @Test
    void login_whenLocalProfileIsDisabled_rejectsAccess() {
        assertInactiveStatusIsRejected(AccountStatus.DISABLED);
    }

    @Test
    void login_whenLocalUserIsLocked_rejectsAccess() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.ACTIVE);
        user.setAccountNonLocked(false);

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountAccessDeniedException.class)
                .hasMessage("Account is not allowed to access the platform");

        verify(authenticationEventLogger).loginFailed("LOCAL_ACCOUNT_NOT_ACTIVE", "student@gmail.com", "keycloak-student-id");
    }

    @Test
    void login_whenPrivilegedRoleMismatch_rejectsAccessAndLogsSecurityEvent() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-admin-id", "admin@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.ADMIN, AccountStatus.ACTIVE);
        user.setAdminProfile(new AdminProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-admin-id")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountAccessDeniedException.class)
                .hasMessage("Account role is not consistent");

        verify(authenticationEventLogger).loginFailed("PRIVILEGED_ROLE_MISMATCH", "admin@gmail.com", "keycloak-admin-id");
    }

    @Test
    void login_whenKeycloakUsesSuperadminSpelling_matchesLocalSuperAdminRole() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-super-id", "super@gmail.com", Set.of("SUPERADMIN"));
        User user = activeUser(Role.SUPER_ADMIN, AccountStatus.ACTIVE);
        user.setAdminProfile(new AdminProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-super-id")).thenReturn(Optional.of(user));

        LoginResponse response = authenticationService.login(request);

        assertThat(response.getUser().getRole()).isEqualTo(Role.SUPER_ADMIN);
    }

    @Test
    void login_whenAuthenticationEventLoggingRuns_doesNotPassPasswordOrTokensToLogger() {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, AccountStatus.ACTIVE);
        user.setStudentProfile(new StudentProfile());

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        authenticationService.login(request);

        verify(authenticationEventLogger, never()).loginFailed(any(), any(), any());
        verify(authenticationEventLogger).loginSucceeded(user.getId(), "keycloak-student-id", Role.STUDENT);
    }

    private void assertInactiveStatusIsRejected(AccountStatus accountStatus) {
        LoginRequest request = loginRequest();
        KeycloakAuthenticationResult keycloakSession = keycloakSession("keycloak-student-id", "student@gmail.com", Set.of("STUDENT"));
        User user = activeUser(Role.STUDENT, accountStatus);

        when(keycloakAuthenticationClient.authenticate(request)).thenReturn(keycloakSession);
        when(userRepository.findByKeycloakUserId("keycloak-student-id")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(AccountAccessDeniedException.class)
                .hasMessage("Account is not allowed to access the platform");

        verify(authenticationEventLogger).loginFailed("LOCAL_ACCOUNT_NOT_ACTIVE", "student@gmail.com", "keycloak-student-id");
    }

    private LoginRequest loginRequest() {
        return LoginRequest.builder()
                .identifier("student@gmail.com")
                .password("Password@123")
                .build();
    }

    private KeycloakAuthenticationResult keycloakSession(String keycloakUserId, String email, Set<String> roles) {
        return KeycloakAuthenticationResult.builder()
                .keycloakUserId(keycloakUserId)
                .email(email)
                .realmRoles(roles)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(300L)
                .refreshExpiresIn(1800L)
                .scope("openid email profile")
                .build();
    }

    private User activeUser(Role role, AccountStatus accountStatus) {
        User user = User.builder()
                .keycloakUserId(keycloakUserIdFor(role))
                .email(role == Role.ADMIN ? "admin@gmail.com" : "student@gmail.com")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .accountStatus(accountStatus)
                .accountNonLocked(true)
                .emailVerified(true)
                .build();
        user.setId(UUID.randomUUID());
        if (role == Role.TEACHER) {
            user.setTeacherProfile(new TeacherProfile());
        }
        return user;
    }

    private String keycloakUserIdFor(Role role) {
        if (role == Role.ADMIN) {
            return "keycloak-admin-id";
        }
        if (role == Role.SUPER_ADMIN) {
            return "keycloak-super-id";
        }
        if (role == Role.TEACHER) {
            return "keycloak-teacher-id";
        }
        return "keycloak-student-id";
    }
}
