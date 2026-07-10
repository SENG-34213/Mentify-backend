package com.mentify.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void givenAdminRealmRole_whenConvertingJwt_thenReturnsRoleAdminAuthority() {
        // Arrange
        Jwt jwt = jwtWithRealmRoles(List.of("ADMIN"));

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void givenStudentRealmRole_whenConvertingJwt_thenReturnsRoleStudentAuthority() {
        // Arrange
        Jwt jwt = jwtWithRealmRoles(List.of("STUDENT"));

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void givenMultipleRealmRoles_whenConvertingJwt_thenReturnsSpringAuthorities() {
        // Arrange
        Jwt jwt = jwtWithRealmRoles(List.of("ADMIN", "STUDENT"));

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities)
                .extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_STUDENT");
    }

    @Test
    void givenMissingRealmAccessClaim_whenConvertingJwt_thenReturnsEmptyAuthorities() {
        // Arrange
        Jwt jwt = baseJwtBuilder().build();

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    @Test
    void givenMissingRolesClaim_whenConvertingJwt_thenReturnsEmptyAuthorities() {
        // Arrange
        Jwt jwt = baseJwtBuilder()
                .claim("realm_access", Map.of("not_roles", List.of("ADMIN")))
                .build();

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertThat(authorities).isEmpty();
    }

    private Jwt jwtWithRealmRoles(List<String> roles) {
        return baseJwtBuilder()
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    private Jwt.Builder baseJwtBuilder() {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}
