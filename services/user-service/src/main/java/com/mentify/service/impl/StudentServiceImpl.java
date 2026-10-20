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
import com.mentify.service.AuthCredentialService;
import com.mentify.service.StudentService;
import com.mentify.service.registration.AuthUserProvisionRequest;
import com.mentify.service.registration.RegistrationEmailDispatcher;
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
    private final AuthCredentialService authCredentialService;
    private final RegistrationEmailDispatcher registrationEmailDispatcher;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Override
    @Transactional
    public ApiResponse<UserResponse> registerStudent(StudentRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        String authUserId = null;
        String temporaryPassword = temporaryPasswordGenerator.generate();
        try {
            authUserId = authCredentialService.createUser(new AuthUserProvisionRequest(
                    request.getEmail(), request.getFirstName(), request.getLastName()));
            authCredentialService.setInitialPassword(authUserId, temporaryPassword);

            User user = studentMapper.toUserEntity(request);
            user.setAuthUserId(authUserId);
            user.setAccountStatus(AccountStatus.INVITED);
            user.setEmailVerified(true);
            StudentProfile profile = studentMapper.toStudentProfileEntity(request);
            Address address = studentMapper.toAddressEntity(request.getAddress());

            user.setStudentProfile(profile);
            user.setAddress(address);

            User savedUser = userRepository.saveAndFlush(user);
            authCredentialService.applyPendingPassword(authUserId);
            registrationEmailDispatcher.sendAfterCommit(
                    authUserId, request.getEmail(), request.getFirstName(), temporaryPassword);

            UserResponse userResponse = studentMapper.toUserResponse(savedUser);

            return ApiResponse.<UserResponse>builder()
                    .message("Student registered. Sign-in credentials will be sent by email.")
                    .data(userResponse)
                    .status(HttpStatus.CREATED)
                    .build();
        } catch (Exception exception) {
            if (authUserId != null) {
                try {
                    authCredentialService.deleteUser(authUserId);
                } catch (Exception cleanupException) {
                    exception.addSuppressed(cleanupException);
                    log.warn("Failed to clean up auth user {} after student registration failure", authUserId);
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
