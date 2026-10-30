package com.mentify.assignment.dto.response;

import com.mentify.assignment.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class StudentAssignmentResponse {
    UUID id;
    UUID courseId;
    UUID moduleId;
    UUID lessonId;
    String title;
    String description;
    String instructions;
    LocalDateTime startDate;
    LocalDateTime dueDate;
    Integer maxMarks;
    String assignmentStatus;
    UUID submissionId;
    SubmissionStatus submissionStatus;
    Integer attemptNumber;
    LocalDateTime submittedAt;
    Boolean allowLateSubmission;
    Boolean gradeReleased;
    BigDecimal marks;
    String feedback;
}
