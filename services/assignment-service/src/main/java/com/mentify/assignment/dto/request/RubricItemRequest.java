package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "Rubric marks are required")
    @Min(value = 0, message = "Rubric marks cannot be negative")
    private Integer marks;
}
