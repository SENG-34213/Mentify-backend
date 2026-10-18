package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssignmentDashboardResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private LocalDateTime dueDate;
    private String status;
    private Long totalSubmissions;
    private Long gradedCount;
}
