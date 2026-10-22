package com.mentify.assignment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignmentAnalyticsSummaryResponse(
        UUID courseId,
        BigDecimal completionRate,
        BigDecimal averageScore,
        BigDecimal missingRate
) {
}
