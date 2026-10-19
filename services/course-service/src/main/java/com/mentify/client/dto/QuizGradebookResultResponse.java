package com.mentify.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class QuizGradebookResultResponse {
    private UUID studentId;
    private BigDecimal score;
    private BigDecimal percentage;
}
