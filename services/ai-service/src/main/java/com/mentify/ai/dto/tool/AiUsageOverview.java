package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiUsageOverview(
        LocalDateTime since,
        long requests,
        long completedRequests,
        long failedRequests,
        long blockedRequests,
        long totalTokens,
        BigDecimal estimatedCost,
        Double averageLatencyMs
) {
}
