package com.mentify.service.authentication;

import com.mentify.enums.Role;

import java.util.UUID;

public interface AuthenticationEventLogger {
    void loginSucceeded(UUID localUserId, String keycloakUserId, Role role);

    void loginFailed(String reason, String identifier, String keycloakUserId);
}
