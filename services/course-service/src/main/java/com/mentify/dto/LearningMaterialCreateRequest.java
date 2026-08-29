package com.mentify.dto;

import com.mentify.enums.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMaterialCreateRequest {

    @NotBlank(message = "Learning material title is required")
    @Size(max = 150, message = "Learning material title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Learning material type is required")
    private MaterialType type;

    @NotBlank(message = "File URL is required")
    @Size(max = 500, message = "File URL must not exceed 500 characters")
    private String fileUrl;

    private UUID lessonId;
}
