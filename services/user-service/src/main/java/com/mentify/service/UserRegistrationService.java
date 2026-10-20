package com.mentify.service;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.dto.UpdateCurrentUserRequest;
import com.mentify.dto.UpdateTeacherProfileRequest;
import com.mentify.dto.ChangePasswordRequest;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.StudentDirectoryProfileResponse;
import com.mentify.dto.StudentLookupRequest;
import com.mentify.dto.StudentLookupResponse;
import com.mentify.entity.User;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.Address;
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
import com.mentify.service.authentication.KeycloakAuthenticationClient;
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
    private final KeycloakAuthenticationClient keycloakAuthenticationClient;

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
    public UserRegistrationResponse updateCurrentUser(String keycloakUserId, UpdateCurrentUserRequest request) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        applyNameUpdate(user, request);
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getBio() != null || request.getAvatarUrl() != null || request.getSchoolName() != null) {
            if (user.getRole() != Role.STUDENT || user.getStudentProfile() == null) {
                throw new InvalidUserStateException("Student profile fields can only be updated by students");
            }
            if (request.getBio() != null) {
                user.getStudentProfile().setBiography(trimToNull(request.getBio()));
            }
            if (request.getAvatarUrl() != null) {
                user.getStudentProfile().setAvatarUrl(trimToNull(request.getAvatarUrl()));
            }
            if (request.getSchoolName() != null) {
                user.getStudentProfile().setSchoolName(trimToNull(request.getSchoolName()));
            }
        }
        if (request.getAddress() != null) {
            Address address = user.getAddress();
            if (address == null) {
                address = Address.builder()
                        .addressLine1(request.getAddress().getAddressLine1().trim())
                        .city(request.getAddress().getCity().trim())
                        .district(request.getAddress().getDistrict().trim())
                        .build();
                user.setAddress(address);
            }
            address.setAddressLine1(request.getAddress().getAddressLine1().trim());
            address.setAddressLine2(trimToNull(request.getAddress().getAddressLine2()));
            address.setCity(request.getAddress().getCity().trim());
            address.setDistrict(request.getAddress().getDistrict().trim());
            address.setPostalCode(trimToNull(request.getAddress().getPostalCode()));
        }
        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            user.setPushNotifications(request.getPushNotifications());
        }
        if (request.getDailyDigestEnabled() != null) {
            user.setDailyDigestEnabled(request.getDailyDigestEnabled());
        }
        if (request.getDailyDigestTime() != null) {
            user.setDailyDigestTime(request.getDailyDigestTime());
        }
        return UserRegistrationResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String keycloakUserId, ChangePasswordRequest request) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var session = keycloakAuthenticationClient.authenticate(LoginRequest.builder()
                .identifier(user.getEmail()).password(request.getCurrentPassword()).build());
        try {
            keycloakUserService.setInitialPassword(user.getKeycloakUserId(), request.getNewPassword());
        } finally {
            if (session.getRefreshToken() != null && !session.getRefreshToken().isBlank()) {
                try {
                    keycloakAuthenticationClient.logout(session.getRefreshToken());
                } catch (RuntimeException logoutException) {
                    log.debug("Could not close temporary password-verification session for user {}",
                            user.getKeycloakUserId());
                }
            }
        }
    }

    @Transactional
    public UserRegistrationResponse updateTeacherProfile(
            String keycloakUserId, UpdateTeacherProfileRequest request) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.TEACHER || user.getTeacherProfile() == null) {
            throw new InvalidUserStateException("Current user does not have a teacher profile");
        }
        TeacherProfile profile = user.getTeacherProfile();
        if (request.getSpecializations() != null) {
            List<String> specializations = request.getSpecializations().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim).distinct().toList();
            if (specializations.isEmpty()) {
                throw new InvalidUserStateException("At least one specialization is required");
            }
            profile.setSpecializations(specializations);
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber().trim());
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getTitle() != null) profile.setAcademicTitle(request.getTitle().trim());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment().trim());
        if (request.getOfficeLocation() != null) profile.setOfficeLocation(request.getOfficeLocation().trim());
        if (request.getOfficeHours() != null) profile.setOfficeHours(request.getOfficeHours().trim());
        if (request.getBio() != null) profile.setBiography(request.getBio().trim());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl().trim());
        return UserRegistrationResponse.from(userRepository.save(user));
    }

    private void applyNameUpdate(User user, UpdateCurrentUserRequest request) {
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            String[] parts = request.getDisplayName().trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            user.setLastName(parts.length > 1 ? parts[1] : parts[0]);
        }
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (user.getTeacherProfile() != null) {
            user.getTeacherProfile().setFirstName(user.getFirstName());
            user.getTeacherProfile().setLastName(user.getLastName());
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public List<StudentLookupResponse> lookupStudents(StudentLookupRequest request) {
        List<String> keycloakUserIds = request.getStudentIds().stream()
                .map(UUID::toString)
                .distinct()
                .toList();
        return userRepository.findAllByKeycloakUserIdInAndRole(keycloakUserIds, Role.STUDENT).stream()
                .map(user -> StudentLookupResponse.builder()
                        .studentId(UUID.fromString(user.getKeycloakUserId()))
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build())
                .toList();
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
