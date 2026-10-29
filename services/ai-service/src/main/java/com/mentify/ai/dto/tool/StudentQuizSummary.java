package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentQuizSummary(
        UUID id,
        UUID courseId,
        String title,
        Integer attemptsUsed,
        BigDecimal lastScorePercentage) {
}
