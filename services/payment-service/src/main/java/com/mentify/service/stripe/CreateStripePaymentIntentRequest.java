package com.mentify.service.stripe;

import java.util.UUID;

public record CreateStripePaymentIntentRequest(
        long amountInMinorUnits,
        String currency,
        UUID paymentId,
        UUID studentId,
        UUID courseId,
        String idempotencyKey
) {
}
