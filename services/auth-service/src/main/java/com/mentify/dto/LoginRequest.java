package com.mentify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String identifier;
    @NotBlank private String password;
    private boolean rememberMe = true;
}
