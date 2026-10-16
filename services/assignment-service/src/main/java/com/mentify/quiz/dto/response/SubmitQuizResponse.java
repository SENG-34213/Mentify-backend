package com.mentify.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizResponse {

    private UUID attemptId;
    private UUID quizId;
    private BigDecimal score;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private Boolean passed;
    private LocalDateTime submittedAt;
}
