package com.mentify.controller;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.enums.Role;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.AdminRegistrationService;
import com.mentify.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRegistration {

    private final UserRegistrationService userRegistrationService;
    private final AdminRegistrationService adminRegistrationService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody AdminRegisterUserRequest request
    ) {
        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "User registered successfully. Password setup email will be sent.",
                        response
                )
        );
    }

    @PostMapping("/admins/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerAdmin(
            @Valid @RequestBody SuperAdminRegisterAdminRequest request
    ) {
        UserRegistrationResponse response = adminRegistrationService.registerAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Admin registered successfully. Password setup email will be sent.",
                        response
                )
        );
    }

    @PostMapping("/{userId}/resend-invitation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable UUID userId) {
        userRegistrationService.resendInvitation(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Invitation email sent successfully.",
                        null
                )
        );
    }

    @GetMapping("/teachers/{teacherId}/exists")
    public ResponseEntity<Boolean> teacherExists(@PathVariable UUID teacherId) {
        boolean exists = userRepository.existsByIdAndRole(teacherId, Role.TEACHER);
        return ResponseEntity.ok(exists);
    }
}
