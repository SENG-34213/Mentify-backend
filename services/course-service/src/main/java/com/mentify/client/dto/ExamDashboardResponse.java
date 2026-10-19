package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExamDashboardResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private BigDecimal totalMarks;
}
