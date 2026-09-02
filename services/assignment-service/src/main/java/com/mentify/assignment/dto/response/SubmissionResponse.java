package com.mentify.assignment.dto.response;

import com.mentify.assignment.enums.SubmissionStatus;
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
public class SubmissionResponse {
    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private Integer attemptNumber;
    private SubmissionStatus status;
    private String content;
    private Boolean isLate;
    private LocalDateTime submittedAt;
    private BigDecimal marks;
    private String feedback;
    private LocalDateTime gradedAt;
    private UUID gradedBy;
    private LocalDateTime returnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
