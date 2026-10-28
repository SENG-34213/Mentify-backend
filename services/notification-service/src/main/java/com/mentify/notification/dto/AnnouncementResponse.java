package com.mentify.notification.dto;

import com.mentify.notification.entity.AnnouncementAudience;
import com.mentify.notification.entity.AnnouncementPriority;
import com.mentify.notification.entity.AnnouncementStatus;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String title,
        String content,
        AnnouncementAudience audience,
        AnnouncementPriority priority,
        AnnouncementStatus status,
        Instant publishAt,
        Instant expiresAt,
        Instant publishedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        boolean read
) {
}
