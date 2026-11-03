package com.mentify.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentAnalyticsOverviewResponse(
        BigDecimal revenue,
        long successfulPayments,
        long failedPayments,
        long refundedPayments,
        Map<String, BigDecimal> revenueTrend
) {
    public PaymentAnalyticsOverviewResponse(
            BigDecimal revenue,
            long successfulPayments,
            long failedPayments,
            long refundedPayments
    ) {
        this(revenue, successfulPayments, failedPayments, refundedPayments, Map.of());
    }
}
