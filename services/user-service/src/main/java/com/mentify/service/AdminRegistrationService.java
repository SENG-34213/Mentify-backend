package com.mentify.service;

import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.PasswordSetupEmailDispatcher;
import com.mentify.service.registration.UserRegistrationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserRegistrationFactory userRegistrationFactory;
    private final PasswordSetupEmailDispatcher passwordSetupEmailDispatcher;

    @Transactional
    public UserRegistrationResponse registerAdmin(SuperAdminRegisterAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String keycloakUserId = null;

        try {
            keycloakUserId = keycloakUserService.createUser(KeycloakUserProvisionRequest.from(request));

            keycloakUserService.assignRealmRole(keycloakUserId, Role.ADMIN.name());

            User user = userRegistrationFactory.createAdmin(request, keycloakUserId);
            User savedUser = userRepository.saveAndFlush(user);
            passwordSetupEmailDispatcher.sendAfterCommit(keycloakUserId);

            return UserRegistrationResponse.from(savedUser);
        } catch (Exception exception) {
            compensateKeycloakUserCreation(keycloakUserId, exception);
            throw exception;
        }
    }

    private void compensateKeycloakUserCreation(String keycloakUserId, Exception originalException) {
        if (keycloakUserId == null) {
            return;
        }

        try {
            keycloakUserService.deleteUser(keycloakUserId);
        } catch (Exception deleteException) {
            originalException.addSuppressed(deleteException);
            log.warn("Failed to delete Keycloak user {} during admin registration compensation", keycloakUserId, deleteException);
        }
    }
}
