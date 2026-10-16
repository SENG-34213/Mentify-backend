package com.mentify.service.stripe;

import java.util.UUID;

public record CreateStripeRefundRequest(
        String paymentIntentId,
        UUID paymentId,
        String idempotencyKey,
        String reason
) {
}
