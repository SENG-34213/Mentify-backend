package com.mentify.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConvertersTest {

    private final Jwt token = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("subject-1")
            .claim("preferred_username", "jeyar")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

    @Test
    void audienceValidatorAcceptsRequiredAudienceAndRejectsMissingAudience() {
        JwtAudienceValidator validator = new JwtAudienceValidator("mentify-api");
        Jwt valid = Jwt.withTokenValue("valid").header("alg", "none")
                .audience(List.of("mentify-api")).build();
        Jwt invalid = Jwt.withTokenValue("invalid").header("alg", "none")
                .audience(List.of("other-api")).build();

        assertThat(validator.validate(valid).hasErrors()).isFalse();
        assertThat(validator.validate(invalid).hasErrors()).isTrue();
    }

    @Test
    void roleConverterSupportsDirectRolesAndRealmAccessRoles() {
        JwtRoleConverter converter = new JwtRoleConverter();
        Jwt direct = Jwt.withTokenValue("direct").header("alg", "none")
                .claim("roles", List.of("ADMIN", "ROLE_TEACHER", " ")).build();
        Jwt realm = Jwt.withTokenValue("realm").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("STUDENT"))).build();

        assertThat(converter.convert(direct)).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TEACHER");
        assertThat(converter.convert(realm)).extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void authenticationConverterUsesConfiguredPrincipalAndFallsBackToSubject() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(new JwtRoleConverter());

        assertThat(converter.convert(token).getName()).isEqualTo("jeyar");

        Jwt withoutPrincipal = Jwt.withTokenValue("fallback").header("alg", "none")
                .subject("subject-2").build();
        assertThat(converter.convert(withoutPrincipal).getName()).isEqualTo("subject-2");
    }
}
