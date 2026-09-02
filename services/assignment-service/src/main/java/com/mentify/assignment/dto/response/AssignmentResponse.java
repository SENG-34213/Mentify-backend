package com.mentify.assignment.dto.response;

import com.mentify.assignment.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private UUID id;
    private UUID courseId;
    private UUID moduleId;
    private UUID lessonId;
    private UUID teacherId;
    private String title;
    private String description;
    private String instructions;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private Integer maxMarks;
    private Integer allowedAttempts;
    private Boolean allowLateSubmission;
    private Integer latePenaltyPercentage;
    private AssignmentStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
