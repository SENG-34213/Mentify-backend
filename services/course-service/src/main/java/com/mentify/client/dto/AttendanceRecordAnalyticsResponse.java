package com.mentify.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AttendanceRecordAnalyticsResponse {
    private UUID studentId;
    private String status;
}
