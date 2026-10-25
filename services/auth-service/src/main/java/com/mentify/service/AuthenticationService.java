package com.mentify.service;

import com.mentify.client.UserProfileClient;
import com.mentify.config.AuthProperties;
import com.mentify.dto.AuthResponse;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.ProvisionUserRequest;
import com.mentify.entity.AuthUser;
import com.mentify.repository.AuthUserRepository;
import com.mentify.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthUserRepository authUsers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileClient profiles;
    private final JwtTokenService tokens;
    private final AuthProperties authProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthUser user = authUsers.findByEmailIgnoreCase(request.getIdentifier().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (isLocked(user, now)) {
            throw new IllegalArgumentException(
                    "Too many unsuccessful login attempts. Please try again later or reset your password.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerFailedAttempt(user, now);
            throw new IllegalArgumentException("Invalid email or password");
        }
        UserProfileClient.AuthProfile profile = profiles.findByEmail(user.getEmail());
        if ("SUSPENDED".equals(profile.getAccountStatus())) {
            throw new IllegalStateException("Your account is suspended. Please contact an administrator.");
        }
        if (!profile.isActive() || !profile.isAccountNonLocked()) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        if ("INVITED".equals(profile.getAccountStatus())) {
            profile = profiles.activateInvitedUser(user.getEmail());
        } else if (!"ACTIVE".equals(profile.getAccountStatus())) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        clearLock(user);
        user.setLastLoginAt(now);
        authUsers.save(user);
        profiles.recordLastLogin(user.getEmail());
        return response(tokens.issue(user, profile, request.isRememberMe()), profile, user.isMustChangePassword());
    }

    @Transactional
    public void provision(ProvisionUserRequest request) {
        AuthUser user = authUsers.findById(request.getUserId()).orElseGet(AuthUser::new);
        user.setUserId(request.getUserId());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setLocked(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setMustChangePassword(request.isMustChangePassword());
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authUsers.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        AuthUser user = authUsers.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid current password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        clearLock(user);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authUsers.save(user);
    }

    @Transactional
    public void unlock(UUID userId) {
        AuthUser user = authUsers.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        clearLock(user);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authUsers.save(user);
    }

    @Transactional
    public boolean isAccountLocked(UUID userId) {
        AuthUser user = authUsers.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        return isLocked(user, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void delete(UUID userId) {
        refreshTokens.deleteAllByUserId(userId);
        // Deletion is intentionally idempotent. User-service may retry cleanup
        // after a partially completed registration or a previous delete attempt.
        authUsers.findById(userId).ifPresent(authUsers::delete);
    }

    @Transactional
    public AuthResponse refresh(String raw) {
        JwtTokenService.RefreshSession session = tokens.rotate(raw);
        AuthUser user = authUsers.findById(session.userId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!user.isEnabled()
                || isLocked(user, LocalDateTime.now(ZoneOffset.UTC))
                || user.isMustChangePassword()) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        UserProfileClient.AuthProfile profile = profiles.findByEmail(user.getEmail());
        if (!profile.isActive()
                || !profile.isAccountNonLocked()
                || !"ACTIVE".equals(profile.getAccountStatus())) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        return response(tokens.issue(user, profile, session.rememberMe()), profile, user.isMustChangePassword());
    }

    private boolean isLocked(AuthUser user, LocalDateTime now) {
        if (!user.isLocked()) {
            return false;
        }
        if (user.getLockedUntil() != null && !now.isBefore(user.getLockedUntil())) {
            clearLock(user);
            authUsers.save(user);
            return false;
        }
        return true;
    }

    private void registerFailedAttempt(AuthUser user, LocalDateTime now) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= Math.max(1, authProperties.getMaxFailedAttempts())) {
            user.setLocked(true);
            user.setLockedUntil(now.plusMinutes(Math.max(1, authProperties.getLockDurationMinutes())));
            refreshTokens.deleteAllByUserId(user.getUserId());
        }
        authUsers.save(user);
    }

    private void clearLock(AuthUser user) {
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setLockedUntil(null);
    }

    private AuthResponse response(JwtTokenService.IssuedTokens issued, UserProfileClient.AuthProfile profile,
                                  boolean mustChangePassword) {
        return AuthResponse.builder()
                .accessToken(issued.accessToken())
                .refreshToken(issued.refreshToken())
                .tokenType("Bearer")
                .expiresIn(issued.expiresIn())
                .refreshExpiresIn(issued.refreshExpiresIn())
                .rememberMe(issued.rememberMe())
                .scope("profile")
                .issuedAt(java.time.Instant.now())
                .mustChangePassword(mustChangePassword)
                .profile(AuthResponse.Profile.builder()
                        .userId(profile.getUserId())
                        .email(profile.getEmail())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .role(profile.getRole())
                        .accountStatus(profile.getAccountStatus())
                        .emailVerified(profile.isEmailVerified())
                        .profileComplete(profile.isProfileComplete())
                        .mustChangePassword(mustChangePassword)
                        .build())
                .build();
    }
}
