package com.mentify.service;

import com.mentify.client.UserProfileClient;
import com.mentify.config.AuthProperties;
import com.mentify.entity.AuthUser;
import com.mentify.entity.RefreshToken;
import com.mentify.repository.RefreshTokenRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;

@Service
public class JwtTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final AuthProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final RSAKey signingKey;
    private final JwtEncoder encoder;

    public JwtTokenService(AuthProperties properties, RefreshTokenRepository refreshTokens,
                           RSAKey signingKey, JwtEncoder encoder) {
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.signingKey = signingKey;
        this.encoder = encoder;
    }

    public IssuedTokens issue(AuthUser user, UserProfileClient.AuthProfile profile) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(properties.getAccessTokenSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience())).subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(expires)
                .claim("email", profile.getEmail())
                .claim("preferred_username", profile.getEmail())
                .claim("roles", List.of(profile.getRole()))
                .claim("scope", "profile")
                .claim("token_use", "access")
                .claim("jti", UUID.randomUUID().toString())
                .build();
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256)
                        .keyId(signingKey.getKeyID())
                        .type("JWT")
                        .build(), claims)).getTokenValue();
        String refresh = randomToken();
        RefreshToken entity = new RefreshToken();
        entity.setId(UUID.randomUUID());
        entity.setUserId(user.getUserId());
        entity.setTokenHash(hash(refresh));
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setExpiresAt(
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(properties.getRefreshTokenSeconds())
        );
        refreshTokens.save(entity);
        return new IssuedTokens(access, refresh, properties.getAccessTokenSeconds(), properties.getRefreshTokenSeconds(), expires);
    }

    public UUID rotate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        RefreshToken token = refreshTokens.findByTokenHash(hash(raw))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
        if (token.getRevokedAt() != null
                || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        token.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokens.save(token);
        return token.getUserId();
    }

    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(hash(raw)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
                refreshTokens.save(token);
            }
        });
    }

    public Map<String, Object> jwks() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            long expiresIn,
            long refreshExpiresIn,
            Instant expiresAt
    ) {
    }
}
