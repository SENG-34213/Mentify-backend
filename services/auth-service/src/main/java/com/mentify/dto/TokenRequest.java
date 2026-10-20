package com.mentify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {
    @NotBlank private String refreshToken;
}
