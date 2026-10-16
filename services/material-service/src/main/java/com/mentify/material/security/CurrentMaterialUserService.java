package com.mentify.material.security;

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
public class CurrentMaterialUserService {

    public UUID getCurrentUserId() {
        return getCurrentUser().userId();
    }

    public AuthenticatedMaterialUser getCurrentUser() {
        Jwt jwt = getCurrentJwt();
        UUID userId = resolveUserId(jwt);
        Authentication authentication = getAuthentication();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedMaterialUser(userId, roles);
    }

    private UUID resolveUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            subject = jwt.getClaimAsString("local_user_id");
        }
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("Authenticated user identity is missing");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Authenticated user identity is invalid");
        }
    }

    private Jwt getCurrentJwt() {
        Authentication authentication = getAuthentication();
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
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
