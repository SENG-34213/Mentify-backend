package com.mentify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email
) {}
