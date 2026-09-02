package com.mentify.quiz.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentUserService {

    private static final String LOCAL_USER_ID_CLAIM = "local_user_id";
    private static final String ROLE_PREFIX = "ROLE_";

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

    public boolean hasAnyRole(String... roles) {
        Set<String> requestedRoles = Arrays.stream(roles)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role)
                .collect(Collectors.toSet());

        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requestedRoles::contains);
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
