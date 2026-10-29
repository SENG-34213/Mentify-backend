package com.mentify.service;

import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.AuthUserProvisionRequest;
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
    private final AuthCredentialService authCredentialService;
    private final UserRegistrationFactory userRegistrationFactory;
    private final RegistrationEmailDispatcher registrationEmailDispatcher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional
    public UserRegistrationResponse registerAdmin(SuperAdminRegisterAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String authUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();

        try {
            authUserId = authCredentialService.createUser(new AuthUserProvisionRequest(request.getEmail(), request.getFirstName(), request.getLastName()));
            authCredentialService.setInitialPassword(authUserId, temporaryPassword);

            User user = userRegistrationFactory.createAdmin(request, authUserId);
            user.setEmailVerified(true);
            User savedUser = userRepository.saveAndFlush(user);
            authCredentialService.applyPendingPassword(authUserId);
            registrationEmailDispatcher.sendAfterCommit(
                    authUserId, request.getEmail(), request.getFirstName(), temporaryPassword, Role.ADMIN);

            return UserRegistrationResponse.from(savedUser);
        } catch (Exception exception) {
            compensateAuthUserCreation(authUserId, exception);
            throw exception;
        }
    }

    private void compensateAuthUserCreation(String authUserId, Exception originalException) {
        if (authUserId == null) {
            return;
        }

        try {
            authCredentialService.deleteUser(authUserId);
        } catch (Exception deleteException) {
            originalException.addSuppressed(deleteException);
            log.warn("Failed to delete auth user {} during admin registration compensation", authUserId, deleteException);
        }
    }
}
