package com.mentify.service;

import com.mentify.client.UserProfileClient;
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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthUser user = authUsers.findByEmailIgnoreCase(request.getIdentifier().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!user.isEnabled()
                || user.isLocked()
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setLocked(true);
            }
            authUsers.save(user);
            throw new IllegalArgumentException("Invalid email or password");
        }
        UserProfileClient.AuthProfile profile = profiles.findByEmail(user.getEmail());
        if (!profile.isActive() || !profile.isAccountNonLocked()) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        if ("INVITED".equals(profile.getAccountStatus())) {
            profile = profiles.activateInvitedUser(user.getEmail());
        } else if (!"ACTIVE".equals(profile.getAccountStatus())) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        user.setFailedAttempts(0);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        authUsers.save(user);
        profiles.recordLastLogin(user.getEmail());
        return response(tokens.issue(user, profile), profile, user.isMustChangePassword());
    }

    @Transactional
    public void provision(ProvisionUserRequest request) {
        AuthUser user = authUsers.findById(request.getUserId()).orElseGet(AuthUser::new);
        user.setUserId(request.getUserId());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setLocked(false);
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
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authUsers.save(user);
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
        UUID userId = tokens.rotate(raw);
        AuthUser user = authUsers.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!user.isEnabled() || user.isLocked() || user.isMustChangePassword()) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        UserProfileClient.AuthProfile profile = profiles.findByEmail(user.getEmail());
        if (!profile.isActive()
                || !profile.isAccountNonLocked()
                || !"ACTIVE".equals(profile.getAccountStatus())) {
            throw new IllegalStateException("Account is not allowed to access the platform");
        }
        return response(tokens.issue(user, profile), profile, user.isMustChangePassword());
    }

    private AuthResponse response(JwtTokenService.IssuedTokens issued, UserProfileClient.AuthProfile profile,
                                  boolean mustChangePassword) {
        return AuthResponse.builder()
                .accessToken(issued.accessToken())
                .refreshToken(issued.refreshToken())
                .tokenType("Bearer")
                .expiresIn(issued.expiresIn())
                .refreshExpiresIn(issued.refreshExpiresIn())
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
