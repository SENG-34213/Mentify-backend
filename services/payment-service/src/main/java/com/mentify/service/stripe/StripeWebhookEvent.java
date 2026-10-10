package com.mentify.service.stripe;

import java.time.LocalDateTime;

public record StripeWebhookEvent(
        String eventId,
        String type,
        String paymentIntentId,
        LocalDateTime createdAt
) {
}
