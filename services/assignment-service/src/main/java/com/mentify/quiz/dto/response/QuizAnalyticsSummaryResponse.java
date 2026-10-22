package com.mentify.quiz.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record QuizAnalyticsSummaryResponse(
        UUID courseId,
        BigDecimal completionRate,
        BigDecimal averageScore,
        BigDecimal missingRate
) {
}
