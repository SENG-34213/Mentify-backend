package com.mentify.service;

import com.mentify.service.registration.AuthUserProvisionRequest;

public interface AuthCredentialService {
    String createUser(AuthUserProvisionRequest request);
    void setInitialPassword(String userId, String password);
    void changePassword(String userId, String currentPassword, String newPassword);
    void applyPendingPassword(String userId);
    void deleteUser(String userId);
}
