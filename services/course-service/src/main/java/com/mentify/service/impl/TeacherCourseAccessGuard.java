package com.mentify.service.impl;

import com.mentify.entity.Course;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TeacherCourseAccessGuard {

    public void assertTeacherOwnsCourse(Course course) {
        UUID authenticatedTeacherId = getAuthenticatedTeacherId();
        if (!course.getAssignedTeacherId().equals(authenticatedTeacherId)) {
            throw new AccessDeniedException("Teacher can only manage own courses");
        }
    }

    private UUID getAuthenticatedTeacherId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid teacher identity");
        }
    }
}
