package com.mentify.exam.dto.response;

import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
public class ExamResponse {
    UUID id;
    UUID courseId;
    String title;
    String description;
    LocalDate examDate;
    LocalTime startTime;
    LocalTime endTime;
    BigDecimal totalMarks;
    BigDecimal passMarks;
    ExamType type;
    ExamStatus status;
    UUID createdBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
