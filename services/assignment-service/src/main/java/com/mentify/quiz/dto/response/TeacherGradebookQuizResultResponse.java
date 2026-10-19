package com.mentify.quiz.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookQuizResultResponse {
    UUID studentId;
    BigDecimal score;
    BigDecimal percentage;
    LocalDateTime submittedAt;
}
