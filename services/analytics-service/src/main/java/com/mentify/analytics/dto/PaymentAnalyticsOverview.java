package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentAnalyticsOverview(
        BigDecimal revenue,
        long successfulPayments,
        long failedPayments,
        long refundedPayments,
        Map<String, BigDecimal> revenueTrend
) {
    public PaymentAnalyticsOverview(
            BigDecimal revenue,
            long successfulPayments,
            long failedPayments,
            long refundedPayments
    ) {
        this(revenue, successfulPayments, failedPayments, refundedPayments, Map.of());
    }
}
