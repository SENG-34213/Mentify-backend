package com.mentify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeMonthlyFeeRequest {

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.01", message = "Monthly fee must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Monthly fee can have at most 2 decimal places")
    private BigDecimal monthlyFee;

    @Pattern(regexp = "(?i)[A-Z]{3}", message = "Currency code must be a 3-letter code")
    @Builder.Default
    private String currencyCode = "LKR";
}
