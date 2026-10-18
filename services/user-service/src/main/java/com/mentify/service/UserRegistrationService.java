package com.mentify.service;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.dto.StudentDirectoryProfileResponse;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.exception.InvalidUserStateException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.RegistrationEmailDispatcher;
import com.mentify.service.registration.RegistrationRequestValidator;
import com.mentify.service.registration.TemporaryPasswordGenerator;
import com.mentify.service.registration.UserRegistrationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final RegistrationRequestValidator registrationRequestValidator;
    private final UserRegistrationFactory userRegistrationFactory;
    private final RegistrationEmailDispatcher registrationEmailDispatcher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional
    public UserRegistrationResponse registerUser(AdminRegisterUserRequest request) {
        registrationRequestValidator.validate(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String keycloakUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();

        try {
            keycloakUserId = keycloakUserService.createUser(KeycloakUserProvisionRequest.from(request));
            keycloakUserService.setInitialPassword(keycloakUserId, temporaryPassword);

            keycloakUserService.assignRealmRole(keycloakUserId, request.getRole().name());

            User user = userRegistrationFactory.create(request, keycloakUserId);
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

    @Transactional(readOnly = true)
    public List<UserRegistrationResponse> getUsersByRole(Role role) {
        return userRepository.findAllByRoleOrderByCreatedAtDesc(role).stream()
                .map(UserRegistrationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDirectoryProfileResponse> getStudentsByIds(List<UUID> ids) {
        return userRepository.findAllByIdInAndRole(ids, Role.STUDENT).stream()
                .map(user -> StudentDirectoryProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .studentNumber(user.getStudentProfile() != null
                                ? user.getStudentProfile().getStudentId() : null)
                        .avatar(null)
                        .program(null)
                        .status(!user.isActive() ? "INACTIVE"
                                : user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UserRegistrationResponse getCurrentUser(String keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserRegistrationResponse.from(user);
    }

    @Transactional
    public void resendInvitation(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountStatus() != AccountStatus.INVITED) {
            throw new InvalidUserStateException("Invitation can only be resent for invited users");
        }

        if (user.getKeycloakUserId() == null || user.getKeycloakUserId().isBlank()) {
            throw new InvalidUserStateException("User is not linked to Keycloak");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        keycloakUserService.setInitialPassword(user.getKeycloakUserId(), temporaryPassword);
        registrationEmailDispatcher.sendAfterCommit(
                user.getKeycloakUserId(), user.getEmail(), user.getFirstName(), temporaryPassword);
    }

    @Transactional
    public void updateUserStatus(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(active);
        userRepository.save(user);
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
