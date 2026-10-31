package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricItemRequest {

    @NotBlank(message = "Rubric criterion is required")
    @Size(max = 150, message = "Rubric criterion must not exceed 150 characters")
    private String criterion;

    @NotNull(message = "Rubric percentage weight is required")
    @DecimalMin(value = "0.00", message = "Rubric percentage weight cannot be below 0")
    @DecimalMax(value = "100.00", message = "Rubric percentage weight cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Rubric percentage weight supports up to 2 decimal places")
    private BigDecimal percentageWeight;
}
