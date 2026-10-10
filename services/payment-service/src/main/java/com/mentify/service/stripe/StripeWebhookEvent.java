package com.mentify.service.stripe;

import java.time.LocalDateTime;
import java.util.UUID;

public record StripeWebhookEvent(
        String eventId,
        String type,
        String paymentIntentId,
        UUID paymentId,
        LocalDateTime createdAt
) {
}
