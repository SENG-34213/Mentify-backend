package com.mentify.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter(new KeycloakRoleConverter());
        ReflectionTestUtils.setField(converter, "principalClaim", "preferred_username");
    }

    @Test
    void givenPreferredUsernameClaim_whenConvertingJwt_thenUsesClaimAsPrincipalName() {
        // Arrange
        Jwt jwt = baseJwtBuilder()
                .claim("preferred_username", "admin@mentify.com")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();

        // Act
        var authentication = converter.convert(jwt);

        // Assert
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(authentication.getName()).isEqualTo("admin@mentify.com");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void givenBlankPrincipalClaim_whenConvertingJwt_thenFallsBackToSubject() {
        // Arrange
        Jwt jwt = baseJwtBuilder()
                .claim("preferred_username", " ")
                .build();

        // Act
        var authentication = converter.convert(jwt);

        // Assert
        assertThat(authentication.getName()).isEqualTo("user-123");
    }

    @Test
    void givenCustomPrincipalClaim_whenConvertingJwt_thenUsesConfiguredClaim() {
        // Arrange
        ReflectionTestUtils.setField(converter, "principalClaim", "email");
        Jwt jwt = baseJwtBuilder()
                .claim("preferred_username", "admin")
                .claim("email", "admin@mentify.com")
                .build();

        // Act
        var authentication = converter.convert(jwt);

        // Assert
        assertThat(authentication.getName()).isEqualTo("admin@mentify.com");
    }

    private Jwt.Builder baseJwtBuilder() {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
