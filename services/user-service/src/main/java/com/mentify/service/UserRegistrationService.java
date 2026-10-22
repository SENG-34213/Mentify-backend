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
import com.mentify.service.registration.AuthUserProvisionRequest;
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
    private final AuthCredentialService authCredentialService;
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

        String authUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();

        try {
            authUserId = authCredentialService.createUser(new AuthUserProvisionRequest(request.getEmail(), request.getFirstName(), request.getLastName()));
            authCredentialService.setInitialPassword(authUserId, temporaryPassword);

            User user = userRegistrationFactory.create(request, authUserId);
            user.setEmailVerified(true);
            User savedUser = userRepository.saveAndFlush(user);
            authCredentialService.applyPendingPassword(authUserId);
            registrationEmailDispatcher.sendAfterCommit(
                    authUserId, request.getEmail(), request.getFirstName(), temporaryPassword);

            return UserRegistrationResponse.from(savedUser);
        } catch (Exception exception) {
            compensateAuthUserCreation(authUserId, exception);
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
                        .avatar(user.getStudentProfile() != null
                                ? user.getStudentProfile().getAvatarUrl() : null)
                        .program(null)
                        .status(!user.isActive() ? "INACTIVE"
                                : user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UserRegistrationResponse getCurrentUser(String identityId) {
        User user = findByIdentity(identityId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserRegistrationResponse.from(user);
    }

    @Transactional
    public UserRegistrationResponse updateCurrentUser(String identityId, UpdateCurrentUserRequest request) {
        User user = findByIdentity(identityId)
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
    public void changePassword(String identityId, ChangePasswordRequest request) {
        User user = findByIdentity(identityId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authCredentialService.changePassword(user.getId().toString(), request.getCurrentPassword(), request.getNewPassword());
    }

    @Transactional
    public UserRegistrationResponse updateTeacherProfile(
            String identityId, UpdateTeacherProfileRequest request) {
        User user = findByIdentity(identityId)
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

    private java.util.Optional<User> findByIdentity(String identityId) {
        try {
            return userRepository.findById(UUID.fromString(identityId))
                .or(() -> userRepository.findByAuthUserId(identityId));
        } catch (IllegalArgumentException exception) {
            return userRepository.findByAuthUserId(identityId);
        }
    }

    @Transactional(readOnly = true)
    public List<StudentLookupResponse> lookupStudents(StudentLookupRequest request) {
        List<UUID> requestedIds = request.getStudentIds().stream().distinct().toList();
        List<String> legacyIds = requestedIds.stream().map(UUID::toString).toList();
        List<User> users = new java.util.ArrayList<>(userRepository.findAllByIdInAndRole(requestedIds, Role.STUDENT));
        userRepository.findAllByAuthUserIdInAndRole(legacyIds, Role.STUDENT).stream()
                .filter(candidate -> users.stream().noneMatch(user -> user.getId().equals(candidate.getId())))
                .forEach(users::add);

        return users.stream()
                .map(user -> StudentLookupResponse.builder()
                        .studentId(user.getId() != null
                                ? user.getId()
                                : UUID.fromString(user.getAuthUserId()))
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

        if (user.getAuthUserId() == null || user.getAuthUserId().isBlank()) {
            throw new InvalidUserStateException("User is not linked to the authentication service");
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
            authCredentialService.setInitialPassword(user.getAuthUserId(), temporaryPassword);
        registrationEmailDispatcher.sendAfterCommit(
                    user.getAuthUserId(), user.getEmail(), user.getFirstName(), temporaryPassword);
    }

    @Transactional
    public void updateUserStatus(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
        userRepository.flush();
        // auth-service credentials are provisioned with the user-service UUID.
        // authUserId is the legacy/link identifier and is not the auth record key.
        authCredentialService.deleteUser(user.getId().toString());
    }

    private void compensateAuthUserCreation(String authUserId, Exception originalException) {
        if (authUserId == null) {
            return;
        }

        try {
            authCredentialService.deleteUser(authUserId);
        } catch (Exception deleteException) {
            originalException.addSuppressed(deleteException);
            log.warn("Failed to delete auth user {} during registration compensation", authUserId, deleteException);
        }
    }
}
