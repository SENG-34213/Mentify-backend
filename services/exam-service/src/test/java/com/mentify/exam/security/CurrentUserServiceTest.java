package com.mentify.exam.security;

import com.mentify.exam.enums.ExamRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Jwt jwt, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, granted));
    }

    private Jwt.Builder jwt() {
        return Jwt.withTokenValue("t").header("alg", "none").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void userIdComesFromSubject() {
        UUID id = UUID.randomUUID();
        authenticate(jwt().subject(id.toString()).build());
        assertEquals(id, service.getCurrentUserId());
    }

    @Test
    void userIdFallsBackToLocalUserIdClaim() {
        UUID id = UUID.randomUUID();
        authenticate(jwt().claim("local_user_id", id.toString()).build());
        assertEquals(id, service.getCurrentUserId());
    }

    @Test
    void invalidIdentityIsRejected() {
        authenticate(jwt().subject("not-a-uuid").build());
        assertThrows(AccessDeniedException.class, service::getCurrentUserId);
    }

    @Test
    void missingAuthenticationIsRejected() {
        assertThrows(AccessDeniedException.class, service::getCurrentUserId);
    }

    @Test
    void rolesAreMappedAndUnknownIgnored() {
        authenticate(jwt().subject(UUID.randomUUID().toString()).build(), "ROLE_STUDENT", "ROLE_SUPER_ADMIN", "ROLE_other");
        assertEquals(Set.of(ExamRole.STUDENT, ExamRole.SUPER_ADMIN), service.getCurrentRoles());
        assertTrue(service.hasAnyRole(ExamRole.STUDENT));
        assertFalse(service.hasAnyRole(ExamRole.TEACHER, ExamRole.ADMIN));
    }
}
