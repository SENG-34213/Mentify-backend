package com.mentify.service;

import com.mentify.client.UserProfileClient;
import com.mentify.config.AuthProperties;
import com.mentify.entity.AuthUser;
import com.mentify.entity.RefreshToken;
import com.mentify.repository.RefreshTokenRepository;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {
    private static final long REMEMBERED_REFRESH_SECONDS = 2_592_000;
    private static final long SESSION_REFRESH_SECONDS = 3_600;

    private RefreshTokenRepository refreshTokens;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.setRefreshTokenSeconds(REMEMBERED_REFRESH_SECONDS);
        properties.setSessionRefreshTokenSeconds(SESSION_REFRESH_SECONDS);

        refreshTokens = mock(RefreshTokenRepository.class);
        RSAKey signingKey = mock(RSAKey.class);
        when(signingKey.getKeyID()).thenReturn("test-key");
        JwtEncoder encoder = mock(JwtEncoder.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("access-token");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(refreshTokens.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tokenService = new JwtTokenService(properties, refreshTokens, signingKey, encoder);
    }

    @Test
    void issueUsesRememberedSessionLifetimeWhenRememberMeIsEnabled() {
        JwtTokenService.IssuedTokens issued = tokenService.issue(user(), profile(), true);
        RefreshToken saved = savedToken();

        assertEquals(REMEMBERED_REFRESH_SECONDS, issued.refreshExpiresIn());
        assertTrue(issued.rememberMe());
        assertTrue(saved.isRememberMe());
        assertLifetime(saved, REMEMBERED_REFRESH_SECONDS);
        assertEquals("access-token", issued.accessToken());
    }

    @Test
    void issueUsesShortSessionLifetimeWhenRememberMeIsDisabled() {
        JwtTokenService.IssuedTokens issued = tokenService.issue(user(), profile(), false);
        RefreshToken saved = savedToken();

        assertEquals(SESSION_REFRESH_SECONDS, issued.refreshExpiresIn());
        assertFalse(issued.rememberMe());
        assertFalse(saved.isRememberMe());
        assertLifetime(saved, SESSION_REFRESH_SECONDS);
    }

    @Test
    void rotateReturnsRememberMePreferenceForTheNextRefreshToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setRememberMe(false);
        token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(java.util.Optional.of(token));

        JwtTokenService.RefreshSession session = tokenService.rotate("refresh-token");

        assertEquals(userId, session.userId());
        assertFalse(session.rememberMe());
        assertTrue(token.getRevokedAt() != null);
        verify(refreshTokens).save(token);
    }

    private AuthUser user() {
        AuthUser user = new AuthUser();
        user.setUserId(UUID.randomUUID());
        return user;
    }

    private UserProfileClient.AuthProfile profile() {
        UserProfileClient.AuthProfile profile = mock(UserProfileClient.AuthProfile.class);
        when(profile.getEmail()).thenReturn("student@example.com");
        when(profile.getRole()).thenReturn("STUDENT");
        return profile;
    }

    private RefreshToken savedToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private void assertLifetime(RefreshToken token, long expectedSeconds) {
        long actualMillis = java.time.Duration.between(token.getCreatedAt(), token.getExpiresAt()).toMillis();
        assertTrue(Math.abs(actualMillis - expectedSeconds * 1_000) < 10);
    }
}
