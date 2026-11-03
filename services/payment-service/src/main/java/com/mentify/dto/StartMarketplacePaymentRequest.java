package com.mentify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class StartMarketplacePaymentRequest {
    @NotNull(message = "productId is required")
    private UUID productId;

    @NotBlank(message = "buyerName is required")
    @Size(max = 150, message = "buyerName must not exceed 150 characters")
    private String buyerName;

    @NotBlank(message = "buyerEmail is required")
    @Email(message = "buyerEmail must be valid")
    @Size(max = 320, message = "buyerEmail must not exceed 320 characters")
    private String buyerEmail;

    @NotBlank(message = "buyerPhone is required")
    @Size(max = 40, message = "buyerPhone must not exceed 40 characters")
    private String buyerPhone;
}
