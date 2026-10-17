package com.mentify.service;

import com.mentify.service.registration.KeycloakUserProvisionRequest;

public interface KeycloakUserService {

    String createUser(KeycloakUserProvisionRequest request);

    void setInitialPassword(String keycloakUserId, String initialPassword);

    void assignRealmRole(String keycloakUserId, String roleName);

    void sendPasswordResetEmail(String keycloakUserId);

    void deleteUser(String keycloakUserId);
}
