package com.mentify.service;

import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.RegistrationEmailDispatcher;
import com.mentify.service.registration.TemporaryPasswordGenerator;
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
    private final RegistrationEmailDispatcher registrationEmailDispatcher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional
    public UserRegistrationResponse registerAdmin(SuperAdminRegisterAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String keycloakUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();

        try {
            keycloakUserId = keycloakUserService.createUser(KeycloakUserProvisionRequest.from(request));
            keycloakUserService.setInitialPassword(keycloakUserId, temporaryPassword);

            keycloakUserService.assignRealmRole(keycloakUserId, Role.ADMIN.name());

            User user = userRegistrationFactory.createAdmin(request, keycloakUserId);
            user.setEmailVerified(true);
            User savedUser = userRepository.saveAndFlush(user);
            registrationEmailDispatcher.sendAfterCommit(
                    keycloakUserId, request.getEmail(), request.getFirstName(), temporaryPassword);

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
