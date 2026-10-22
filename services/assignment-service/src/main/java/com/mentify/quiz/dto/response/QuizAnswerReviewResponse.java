package com.mentify.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerReviewResponse {

    private UUID questionId;
    private String questionText;
    private String explanation;
    private UUID selectedOptionId;
    private String selectedAnswer;
    private UUID correctOptionId;
    private String correctAnswer;
    private Boolean correct;
    private BigDecimal awardedMarks;
    private BigDecimal questionMarks;
}
