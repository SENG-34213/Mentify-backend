package com.mentify.service.impl;

import com.mentify.entity.Course;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class TeacherCourseAccessGuard {

    private static final String LOCAL_USER_ID_CLAIM = "local_user_id";

    public boolean isTeacherAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_TEACHER".equals(authority) || "TEACHER".equals(authority));
    }

    public void assertTeacherCanAssign(UUID assignedTeacherId) {
        if (isTeacherAuthenticated() && !getAuthenticatedTeacherId().equals(assignedTeacherId)) {
            throw new AccessDeniedException("Teacher can only assign courses to themselves");
        }
    }

    public void assertTeacherOwnsCourse(Course course) {
        UUID authenticatedTeacherId = getAuthenticatedTeacherId();
        if (!Objects.equals(course.getAssignedTeacherId(), authenticatedTeacherId)) {
            throw new AccessDeniedException("Teacher can only manage own courses");
        }
    }

    public UUID getAuthenticatedTeacherId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }

        String resolvedId = jwt.getClaimAsString(LOCAL_USER_ID_CLAIM);
        if (resolvedId == null || resolvedId.isBlank()) {
            resolvedId = jwt.getSubject();
        }

        if (resolvedId == null || resolvedId.isBlank()) {
            throw new AccessDeniedException("Teacher identity is missing in token");
        }

        try {
            return UUID.fromString(resolvedId);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid teacher identity");
        }
    }
}
