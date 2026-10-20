package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SubmissionDashboardResponse {
    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private String studentName;
    private String status;
    private Integer attemptNumber;
    private BigDecimal marks;
    private LocalDateTime submittedAt;
}
