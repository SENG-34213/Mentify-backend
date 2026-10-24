package com.mentify.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ExamCalendarProjectionRequest {

    @NotNull
    private UUID examId;
    @NotNull
    private UUID courseId;
    @NotBlank
    @Size(max = 200)
    private String title;
    @Size(max = 2000)
    private String description;
    @Size(max = 255)
    private String venue;
    @NotNull
    private LocalDateTime startDateTime;
    @NotNull
    private LocalDateTime endDateTime;
    @NotNull
    private UUID createdBy;
}
