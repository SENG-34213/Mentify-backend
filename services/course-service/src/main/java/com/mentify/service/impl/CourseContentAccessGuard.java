package com.mentify.service.impl;

import com.mentify.client.EntrollmentServiceClient;
import com.mentify.entity.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseContentAccessGuard {

    private static final String LOCAL_USER_ID_CLAIM = "local_user_id";

    private final EntrollmentServiceClient entrollmentServiceClient;

    public void assertCanReadCourseContent(Course course, String authorizationHeader) {
        Authentication authentication = getAuthentication();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
            return;
        }

        UUID userId = getCurrentUserId(authentication);
        if (roles.contains("ROLE_TEACHER")) {
            if (!course.getAssignedTeacherId().equals(userId)) {
                throw new AccessDeniedException("Teacher does not have access to this course");
            }
            return;
        }

        if (roles.contains("ROLE_STUDENT")) {
            if (!entrollmentServiceClient.isStudentEnrolledInCourse(userId, course.getId(), authorizationHeader)) {
                throw new AccessDeniedException("Student is not enrolled in this course");
            }
            return;
        }

        throw new AccessDeniedException("User role is not authorized to access course content");
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication;
    }

    private UUID getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }

        String resolvedId = jwt.getClaimAsString(LOCAL_USER_ID_CLAIM);
        if (resolvedId == null || resolvedId.isBlank()) {
            resolvedId = jwt.getSubject();
        }
        if (resolvedId == null || resolvedId.isBlank()) {
            throw new AccessDeniedException("User identity is missing in token");
        }

        try {
            return UUID.fromString(resolvedId);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid user identity");
        }
    }
}
