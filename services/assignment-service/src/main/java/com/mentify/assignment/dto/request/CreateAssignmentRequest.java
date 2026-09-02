package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.DecimalMax;
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
public class CreateAssignmentRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    private UUID moduleId;

    private UUID lessonId;

    @NotBlank(message = "Assignment title is required")
    @Size(max = 150, message = "Assignment title must not exceed 150 characters")
    private String title;

    private String description;

    private String instructions;

    private LocalDateTime startDate;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    @NotNull(message = "Maximum marks is required")
    @DecimalMin(value = "0.01", message = "Maximum marks must be greater than zero")
    private BigDecimal maxMarks;

    @NotNull(message = "Allowed attempts is required")
    @Min(value = 1, message = "Allowed attempts must be at least 1")
    private Integer allowedAttempts;

    private Boolean lateSubmissionAllowed;

    @DecimalMin(value = "0.00", message = "Late penalty percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Late penalty percentage cannot exceed 100")
    private BigDecimal latePenaltyPercentage;
}
