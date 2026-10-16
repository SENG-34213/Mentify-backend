package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentRequest {

    @NotBlank(message = "Assignment title is required")
    @Size(max = 150, message = "Assignment title must not exceed 150 characters")
    private String title;

    private String description;

    private String instructions;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    @NotNull(message = "Maximum marks is required")
    @Min(value = 1, message = "Maximum marks must be at least 1")
    private Integer maxMarks;

    @NotNull(message = "Allowed attempts is required")
    @Min(value = 1, message = "Allowed attempts must be at least 1")
    private Integer allowedAttempts;

    @NotNull(message = "Late submission flag is required")
    private Boolean allowLateSubmission;

    @Min(value = 0, message = "Late penalty cannot be negative")
    @Max(value = 100, message = "Late penalty cannot exceed 100")
    private Integer latePenaltyPercentage;
}
