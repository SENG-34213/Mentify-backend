package com.mentify.service.stripe;

import java.util.UUID;

public record CreateStripePaymentIntentRequest(
        long amountInMinorUnits,
        String currency,
        UUID paymentId,
        UUID studentId,
        UUID courseId,
        String idempotencyKey,
        UUID productId,
        String paymentType,
        String receiptEmail
) {
    public CreateStripePaymentIntentRequest(long amountInMinorUnits, String currency, UUID paymentId,
                                            UUID studentId, UUID courseId, String idempotencyKey) {
        this(amountInMinorUnits, currency, paymentId, studentId, courseId, idempotencyKey, null, "COURSE", null);
    }
}
