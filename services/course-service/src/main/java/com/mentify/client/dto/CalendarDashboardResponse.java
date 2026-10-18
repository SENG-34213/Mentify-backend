package com.mentify.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CalendarDashboardResponse {
    private UUID id;
    private String title;
    private String eventType;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private UUID courseId;
    private String location;
}
