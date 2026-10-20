package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AttendanceSessionAnalyticsResponse {
    private UUID id;
    private UUID courseId;
    private LocalDate attendanceDate;
    private String status;
    private List<AttendanceRecordAnalyticsResponse> records;
}
