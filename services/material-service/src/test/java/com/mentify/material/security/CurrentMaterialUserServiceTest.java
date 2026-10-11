package com.mentify.material.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentMaterialUserServiceTest {

    private final CurrentMaterialUserService currentUserService = new CurrentMaterialUserService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesUploaderIdFromAuthenticatedJwtSubject() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(userId.toString()));

        assertThat(currentUserService.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void resolvesRolesFromAuthenticatedJwtAuthorities() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(userId.toString()));

        AuthenticatedMaterialUser currentUser = currentUserService.getCurrentUser();

        assertThat(currentUser.userId()).isEqualTo(userId);
        assertThat(currentUser.isTeacher()).isTrue();
        assertThat(currentUser.isAdmin()).isFalse();
    }

    @Test
    void fallsBackToLocalUserIdClaimWhenSubjectIsMissing() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("local_user_id", userId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );

        assertThat(currentUserService.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void rejectsInvalidJwtSubject() {
        SecurityContextHolder.getContext().setAuthentication(authentication("not-a-uuid"));

        assertThatThrownBy(currentUserService::getCurrentUserId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authenticated user identity is invalid");
    }

    private JwtAuthenticationToken authentication(String subject) {
        return new JwtAuthenticationToken(
                jwt(subject),
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
