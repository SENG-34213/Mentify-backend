package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookItemResponse {
    UUID assessmentId;
    String title;
    BigDecimal score;
    BigDecimal maxScore;
    BigDecimal percentage;
    String status;
    BigDecimal marksObtained;
    BigDecimal maxMarks;
    LocalDateTime updatedAt;
}
