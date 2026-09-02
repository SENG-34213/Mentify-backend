package com.mentify.assignment.dto.response;

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
public class StudentAssignmentResponse {

    private UUID id;
    private UUID courseId;
    private UUID moduleId;
    private UUID lessonId;
    private String title;
    private String description;
    private String instructions;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private BigDecimal maxMarks;
    private Integer allowedAttempts;
    private Boolean lateSubmissionAllowed;
    private BigDecimal latePenaltyPercentage;
}
