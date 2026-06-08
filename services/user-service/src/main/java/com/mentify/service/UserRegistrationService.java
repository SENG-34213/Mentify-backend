package com.mentify.service;

import com.mentify.dto.AddressDto;
import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.exception.InvalidRoleException;
import com.mentify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;

    @Transactional
    public UserRegistrationResponse registerUser(AdminRegisterUserRequest request) {
        validateRegistrationRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String keycloakUserId = null;

        try {
            keycloakUserId = keycloakUserService.createUser(request);

            keycloakUserService.assignRealmRole(keycloakUserId, request.getRole().name());
            keycloakUserService.sendPasswordSetupEmail(keycloakUserId);

            User user = buildUser(request, keycloakUserId);
            attachRoleProfile(user, request);
            attachAddress(user, request.getAddress());

            User savedUser = userRepository.save(user);
            return UserRegistrationResponse.from(savedUser);
        } catch (Exception exception) {
            compensateKeycloakUserCreation(keycloakUserId, exception);
            throw exception;
        }
    }

    private User buildUser(AdminRegisterUserRequest request, String keycloakUserId) {
        return User.builder()
                .keycloakUserId(keycloakUserId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .accountStatus(AccountStatus.INVITED)
                .accountNonLocked(true)
                .emailVerified(false)
                .loginAttempts(0)
                .build();
    }

    private void attachRoleProfile(User user, AdminRegisterUserRequest request) {
        if (request.getRole() == Role.STUDENT) {
            AdminRegisterUserRequest.StudentProfileRequest profileRequest = request.getStudentProfile();
            user.setStudentProfile(StudentProfile.builder()
                    .admissionId(generateAdmissionId())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .dateOfBirth(profileRequest.getDateOfBirth())
                    .phoneNumber(request.getPhoneNumber())
                    .guardianName(profileRequest.getGuardianName())
                    .guardianPhone(profileRequest.getGuardianPhone())
                    .attendanceMode(profileRequest.getAttendanceMode())
                    .gradeGroupChatId(profileRequest.getGradeGroupChatId())
                    .build());
            return;
        }

        AdminRegisterUserRequest.TeacherProfileRequest profileRequest = request.getTeacherProfile();
        user.setTeacherProfile(TeacherProfile.builder()
                .teacherCode(generateTeacherCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(profileRequest.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .nic(profileRequest.getNic())
                .specialization(profileRequest.getSpecialization())
                .hireDate(profileRequest.getHireDate())
                .build());
    }

    private void attachAddress(User user, AddressDto addressDto) {
        if (addressDto == null) {
            return;
        }

        user.setAddress(Address.builder()
                .addressLine1(addressDto.getAddressLine1())
                .addressLine2(addressDto.getAddressLine2())
                .city(addressDto.getCity())
                .district(addressDto.getDistrict())
                .postalCode(addressDto.getPostalCode())
                .build());
    }

    private void validateRegistrationRequest(AdminRegisterUserRequest request) {
        if (request.getRole() != Role.STUDENT && request.getRole() != Role.TEACHER) {
            throw new InvalidRoleException("Only STUDENT and TEACHER users can be registered from this endpoint");
        }

        if (request.getAddress() == null) {
            throw new InvalidRoleException("Address details are required when registering a user");
        }

        if (request.getRole() == Role.STUDENT) {
            if (request.getStudentProfile() == null) {
                throw new InvalidRoleException("Student profile details are required when registering a STUDENT");
            }
            if (request.getTeacherProfile() != null) {
                throw new InvalidRoleException("Teacher profile details are not allowed when registering a STUDENT");
            }
            return;
        }

        if (request.getTeacherProfile() == null) {
            throw new InvalidRoleException("Teacher profile details are required when registering a TEACHER");
        }
        if (request.getStudentProfile() != null) {
            throw new InvalidRoleException("Student profile details are not allowed when registering a TEACHER");
        }
    }

    private String generateAdmissionId() {
        return "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTeacherCode() {
        return "TCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
