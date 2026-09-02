package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class GradeSubmissionRequest {

    @NotNull(message = "Marks are required")
    @DecimalMin(value = "0.00", message = "Marks cannot be negative")
    @DecimalMax(value = "100.00", message = "Marks cannot exceed assignment maximum")
    private BigDecimal marks;

    private String feedback;
}
