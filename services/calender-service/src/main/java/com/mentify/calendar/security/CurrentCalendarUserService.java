package com.mentify.calendar.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentCalendarUserService {

    private static final String LOCAL_USER_ID_CLAIM = "local_user_id";

    public UUID getCurrentUserId() {
        Set<UUID> ids = getIdentityIds();
        if (ids.isEmpty()) {
            throw new AccessDeniedException("Authenticated user identity is missing");
        }
        // Persist the canonical local identity when both claims are present.
        Jwt jwt = getCurrentJwt();
        String localId = jwt.getClaimAsString(LOCAL_USER_ID_CLAIM);
        return localId == null || localId.isBlank() ? ids.iterator().next() : parseId(localId);
    }

    /** Both claims identify the authenticated account; legacy records may use either UUID. */
    public Set<UUID> getIdentityIds() {
        Jwt jwt = getCurrentJwt();
        Set<UUID> ids = new LinkedHashSet<>();
        addId(ids, jwt.getClaimAsString(LOCAL_USER_ID_CLAIM));
        addId(ids, jwt.getSubject());
        return Set.copyOf(ids);
    }

    private void addId(Set<UUID> ids, String value) {
        if (value != null && !value.isBlank()) {
            ids.add(parseId(value));
        }
    }

    private UUID parseId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Authenticated user identity is invalid");
        }
    }

    public boolean hasRole(String role) {
        return getRoles().contains("ROLE_" + role);
    }

    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    public Set<String> getRoles() {
        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
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
