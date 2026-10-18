package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SubmissionDashboardResponse {
    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private String studentName;
    private String status;
    private LocalDateTime submittedAt;
}
