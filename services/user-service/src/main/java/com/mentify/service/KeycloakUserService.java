package com.mentify.service;

import com.mentify.service.registration.KeycloakUserProvisionRequest;

public interface KeycloakUserService {

    String createUser(KeycloakUserProvisionRequest request);

    void assignRealmRole(String keycloakUserId, String roleName);

    void sendPasswordSetupEmail(String keycloakUserId);

    void deleteUser(String keycloakUserId);
}
