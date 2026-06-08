package com.mentify.service;

import com.mentify.dto.AdminRegisterUserRequest;

public interface KeycloakUserService {

    String createUser(AdminRegisterUserRequest request);

    void assignRealmRole(String keycloakUserId, String roleName);

    void sendPasswordSetupEmail(String keycloakUserId);

    void deleteUser(String keycloakUserId);
}
