package com.mentify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProvisionUserRequest {
    @NotNull private UUID userId;
    @Email @NotBlank private String email;
    @NotBlank private String password;
}
