package com.mentify.service.impl;

import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.UserResponse;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.mapper.StudentMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.KeycloakUserService;
import com.mentify.service.StudentService;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.PasswordSetupEmailDispatcher;
import com.mentify.service.registration.TemporaryPasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final UserRepository userRepository;
    private final StudentMapper studentMapper;
    private final KeycloakUserService keycloakUserService;
    private final PasswordSetupEmailDispatcher passwordSetupEmailDispatcher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Override
    @Transactional
    public ApiResponse<UserResponse> registerStudent(StudentRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        String keycloakUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();
        try {
            keycloakUserId = keycloakUserService.createUser(new KeycloakUserProvisionRequest(
                    request.getEmail(), request.getFirstName(), request.getLastName()));
            keycloakUserService.setTemporaryPassword(keycloakUserId, temporaryPassword);
            keycloakUserService.assignRealmRole(keycloakUserId, Role.STUDENT.name());

            User user = studentMapper.toUserEntity(request);
            user.setKeycloakUserId(keycloakUserId);
            user.setAccountStatus(AccountStatus.INVITED);
            StudentProfile profile = studentMapper.toStudentProfileEntity(request);
            Address address = studentMapper.toAddressEntity(request.getAddress());

            user.setStudentProfile(profile);
            user.setAddress(address);

            User savedUser = userRepository.saveAndFlush(user);
            passwordSetupEmailDispatcher.sendAfterCommit(
                    keycloakUserId, request.getEmail(), request.getFirstName(), temporaryPassword);

            UserResponse userResponse = studentMapper.toUserResponse(savedUser);

            return ApiResponse.<UserResponse>builder()
                    .message("Student registered. Temporary credentials and a password setup email will be sent.")
                    .data(userResponse)
                    .status(HttpStatus.CREATED)
                    .build();
        } catch (Exception exception) {
            if (keycloakUserId != null) {
                try {
                    keycloakUserService.deleteUser(keycloakUserId);
                } catch (Exception cleanupException) {
                    exception.addSuppressed(cleanupException);
                    log.warn("Failed to clean up Keycloak user {} after student registration failure", keycloakUserId);
                }
            }
            throw exception;
        }
    }

    @Override
    public ApiResponse<Page<UserResponse>> getAllStudents(Pageable pageable) {

        Page<User> students = userRepository.findAllByRole(Role.STUDENT, pageable);
        Page<UserResponse> listOfStudents = students.map(studentMapper::toUserResponse);

        return ApiResponse.<Page<UserResponse>>builder()
                .message("Successfully returned paginated list of Students")
                .data(listOfStudents)
                .status(HttpStatus.OK)
                .build();
    }
}
