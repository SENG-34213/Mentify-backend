package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentAnalyticsOverview(
        BigDecimal revenue,
        long successfulPayments,
        long failedPayments,
        long refundedPayments
) {
}
