package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignmentAnalyticsSummary(
        UUID courseId,
        BigDecimal completionRate,
        BigDecimal averageScore,
        BigDecimal missingRate
) {
}
