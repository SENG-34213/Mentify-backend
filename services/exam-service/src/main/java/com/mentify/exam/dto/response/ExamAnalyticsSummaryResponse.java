package com.mentify.exam.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamAnalyticsSummaryResponse(
        UUID courseId,
        BigDecimal completionRate,
        BigDecimal averageScore,
        BigDecimal missingRate
) {
}
