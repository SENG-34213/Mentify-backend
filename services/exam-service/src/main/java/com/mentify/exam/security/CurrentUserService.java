package com.mentify.exam.security;

import com.mentify.exam.enums.ExamRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves the authenticated user's identity and roles exclusively from the Keycloak JWT.
 * Identifiers such as createdBy / markedBy must always be derived here, never from request payloads.
 */
@Component
public class CurrentUserService {

    private static final String LOCAL_USER_ID_CLAIM = "local_user_id";

    public UUID getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        String resolvedId = jwt.getSubject();
        if (resolvedId == null || resolvedId.isBlank()) {
            resolvedId = jwt.getClaimAsString(LOCAL_USER_ID_CLAIM);
        }

        if (resolvedId == null || resolvedId.isBlank()) {
            throw new AccessDeniedException("Authenticated user identity is missing");
        }

        try {
            return UUID.fromString(resolvedId);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Authenticated user identity is invalid");
        }
    }

    public Set<ExamRole> getCurrentRoles() {
        Set<String> authorities = getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Set<ExamRole> roles = EnumSet.noneOf(ExamRole.class);
        for (ExamRole role : ExamRole.values()) {
            if (authorities.contains(role.authority())) {
                roles.add(role);
            }
        }
        return roles;
    }

    public boolean hasAnyRole(ExamRole... roles) {
        Set<ExamRole> current = getCurrentRoles();
        return Arrays.stream(roles).anyMatch(current::contains);
    }

    private Jwt getCurrentJwt() {
        Object principal = getAuthentication().getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }
        return jwt;
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication;
    }
}
