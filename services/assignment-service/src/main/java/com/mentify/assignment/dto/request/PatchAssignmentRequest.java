package com.mentify.assignment.dto.request;

import jakarta.validation.Valid;
import com.mentify.assignment.enums.AssignmentSubmissionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchAssignmentRequest {

    @Size(max = 150, message = "Assignment title must not exceed 150 characters")
    private String title;

    private String description;
    private String instructions;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;

    @Min(value = 1, message = "Maximum marks must be at least 1")
    private Integer maxMarks;

    @Min(value = 1, message = "Allowed attempts must be at least 1")
    private Integer allowedAttempts;

    private Boolean allowLateSubmission;

    @Min(value = 0, message = "Late penalty cannot be negative")
    @Max(value = 100, message = "Late penalty cannot exceed 100")
    private Integer latePenaltyPercentage;

    private AssignmentSubmissionType submissionType;

    @Valid
    @Size(max = 50, message = "An assignment cannot have more than 50 rubric items")
    private List<RubricItemRequest> rubricItems;

    public boolean isEmpty() {
        return title == null
                && description == null
                && instructions == null
                && startDate == null
                && dueDate == null
                && maxMarks == null
                && allowedAttempts == null
                && allowLateSubmission == null
                && latePenaltyPercentage == null
                && submissionType == null
                && rubricItems == null;
    }
}
