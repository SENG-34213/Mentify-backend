package com.mentify.exam.client.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ExamCalendarProjectionRequest {
    UUID examId;
    UUID courseId;
    String title;
    String description;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    UUID createdBy;
}
