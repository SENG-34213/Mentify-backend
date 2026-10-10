package com.mentify.material.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentMaterialUserService {

    public UUID getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        String subject = jwt.getSubject();
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }
        return jwt;
    }
}
