package com.mentify.controller;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody AdminRegisterUserRequest request
    ) {
        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "User registered successfully. Password setup email sent.",
                        response
                )
        );
    }
}
