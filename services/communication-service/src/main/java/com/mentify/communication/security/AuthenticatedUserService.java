package com.mentify.communication.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthenticatedUserService {

    public UUID getCurrentUserId() {
        return UUID.fromString(getCurrentJwt().getSubject());
    }

    public Set<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Jwt getCurrentJwt() {
        Object principal = getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        throw new AuthenticationCredentialsNotFoundException("Authenticated JWT principal was not found");
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user was not found");
        }

        return authentication;
    }
}
