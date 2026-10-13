package com.mentify.notification.dto;

import com.mentify.notification.entity.EmailDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        String sourceService,
        Instant occurredAt,
        Instant createdAt,
        Instant readAt,
        EmailDeliveryStatus emailStatus
) {
}
