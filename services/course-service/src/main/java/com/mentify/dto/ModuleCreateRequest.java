package com.mentify.dto;

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
public class ModuleCreateRequest {

    @NotBlank(message = "Module title is required")
    @Size(max = 150, message = "Module title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String moduleImage;

    @NotNull(message = "Sequence order is required")
    @Min(value = 1, message = "Sequence order must be at least 1")
    private Integer sequenceOrder;

    @Min(value = 1, message = "Date duration must be at least 1 day")
    private Integer dateDuration;

    private Boolean isVisible;
}
