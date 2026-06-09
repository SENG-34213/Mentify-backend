package com.mentify.service;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.User;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.RegistrationRequestValidator;
import com.mentify.service.registration.UserRegistrationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final RegistrationRequestValidator registrationRequestValidator;
    private final UserRegistrationFactory userRegistrationFactory;

    @Transactional
    public UserRegistrationResponse registerUser(AdminRegisterUserRequest request) {
        registrationRequestValidator.validate(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String keycloakUserId = null;

        try {
            keycloakUserId = keycloakUserService.createUser(request);

            keycloakUserService.assignRealmRole(keycloakUserId, request.getRole().name());
            keycloakUserService.sendPasswordSetupEmail(keycloakUserId);

            User user = userRegistrationFactory.create(request, keycloakUserId);
            User savedUser = userRepository.save(user);
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
            log.warn("Failed to delete Keycloak user {} during registration compensation", keycloakUserId, deleteException);
        }
    }
}
