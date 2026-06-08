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
    void shouldConvertAdminRoleIntoRoleAdmin() {
        Jwt jwt = jwtWithRealmRoles(List.of("ADMIN"));

        var authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldConvertStudentRoleIntoRoleStudent() {
        Jwt jwt = jwtWithRealmRoles(List.of("STUDENT"));

        var authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void shouldConvertMultipleKeycloakRealmRolesToSpringAuthorities() {
        Jwt jwt = jwtWithRealmRoles(List.of("ADMIN", "STUDENT"));

        var authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_STUDENT");
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRealmAccessClaimIsMissing() {
        Jwt jwt = baseJwtBuilder().build();

        var authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesClaimIsMissing() {
        Jwt jwt = baseJwtBuilder()
                .claim("realm_access", Map.of("not_roles", List.of("ADMIN")))
                .build();

        var authorities = converter.convert(jwt);

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
