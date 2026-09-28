package com.mentify.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerationRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 20, message = "Question count must not exceed 20")
    private Integer questionCount;

    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    @NotBlank(message = "Question type is required")
    private String questionType;

    private String userPrompt;
}
