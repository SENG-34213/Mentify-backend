package com.mentify.dto;

import java.math.BigDecimal;

public record PaymentAnalyticsOverviewResponse(
        BigDecimal revenue,
        long successfulPayments,
        long failedPayments,
        long refundedPayments
) {
}
