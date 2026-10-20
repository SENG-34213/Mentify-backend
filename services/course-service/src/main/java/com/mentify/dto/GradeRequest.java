package com.mentify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {

    @NotBlank(message = "Grade name is required")
    @Size(max = 100, message = "Grade name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @jakarta.validation.constraints.NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.01", message = "Monthly fee must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Monthly fee can have at most 2 decimal places")
    private BigDecimal monthlyFee;

    @Pattern(regexp = "(?i)[A-Z]{3}", message = "Currency code must be a 3-letter code")
    @Builder.Default
    private String currencyCode = "LKR";
}
