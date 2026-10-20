package com.mentify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Login identifier is required")
    @Email(message = "Login identifier must be a valid email address")
    @Size(max = 150, message = "Login identifier must not exceed 150 characters")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(max = 256, message = "Password must not exceed 256 characters")
    private String password;
}
