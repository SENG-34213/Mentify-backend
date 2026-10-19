package com.mentify.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class QuizGradebookResponse {
    private UUID quizId;
    private String title;
    private BigDecimal totalMarks;
    private List<QuizGradebookResultResponse> results;
}
