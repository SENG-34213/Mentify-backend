package com.mentify.common.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        String recipientUserId,
        String recipientEmail,
        String type,
        String title,
        String message,
        String sourceService,
        Instant occurredAt
) {
}
