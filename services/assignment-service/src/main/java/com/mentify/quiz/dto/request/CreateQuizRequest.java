package com.mentify.quiz.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotBlank(message = "Quiz title is required")
    @Size(max = 150, message = "Quiz title must not exceed 150 characters")
    private String title;

    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @NotNull(message = "Pass mark is required")
    @DecimalMin(value = "0.00", message = "Pass mark cannot be negative")
    private BigDecimal passMark;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @NotNull(message = "Maximum attempts is required")
    @Min(value = 1, message = "Maximum attempts must be at least 1")
    private Integer maxAttempts;

    private Boolean showResultImmediately;
}
