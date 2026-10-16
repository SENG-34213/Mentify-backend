package com.mentify.material.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedMaterialUser(
        UUID userId,
        Set<String> roles
) {

    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    public boolean isTeacher() {
        return hasRole("TEACHER");
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }

    public boolean hasRole(String role) {
        return roles.contains("ROLE_" + role);
    }
}
