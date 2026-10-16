package com.mentify.service.impl;

import com.mentify.entity.Course;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeacherCourseAccessGuardTest {

    private TeacherCourseAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TeacherCourseAccessGuard();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assertTeacherOwnsCourse_whenSubjectMatches_allowsAccess() {
        UUID teacherId = UUID.randomUUID();
        setJwtAuthentication(teacherId.toString(), UUID.randomUUID().toString());

        Course course = Course.builder().assignedTeacherId(teacherId).build();

        assertThatCode(() -> guard.assertTeacherOwnsCourse(course)).doesNotThrowAnyException();
    }

    @Test
    void assertTeacherOwnsCourse_whenSubjectMissing_fallsBackToLocalUserIdClaim() {
        UUID teacherId = UUID.randomUUID();
        setJwtAuthentication(" ", teacherId.toString());

        Course course = Course.builder().assignedTeacherId(teacherId).build();

        assertThatCode(() -> guard.assertTeacherOwnsCourse(course)).doesNotThrowAnyException();
    }

    @Test
    void assertTeacherOwnsCourse_whenIdsDoNotMatch_throwsAccessDenied() {
        setJwtAuthentication(UUID.randomUUID().toString(), null);

        Course course = Course.builder().assignedTeacherId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> guard.assertTeacherOwnsCourse(course))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Teacher can only manage own courses");
    }

    @Test
    void assertTeacherOwnsCourse_whenAuthenticationIsMissing_throwsAccessDenied() {
        SecurityContextHolder.clearContext();

        Course course = Course.builder().assignedTeacherId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> guard.assertTeacherOwnsCourse(course))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void assertTeacherOwnsCourse_whenPrincipalIsNotJwt_throwsAccessDenied() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Course course = Course.builder().assignedTeacherId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> guard.assertTeacherOwnsCourse(course))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid authentication principal");
    }

    private void setJwtAuthentication(String subject, String localUserId) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (localUserId != null) {
            jwtBuilder.claim("local_user_id", localUserId);
        }

        Jwt jwt = jwtBuilder.build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
        );
    }
}
