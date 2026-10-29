package com.mentify.notification.dto;

import com.mentify.notification.entity.AnnouncementAudience;
import com.mentify.notification.entity.AnnouncementPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotNull AnnouncementAudience audience,
        @NotNull AnnouncementPriority priority,
        @Future Instant publishAt,
        @Future Instant expiresAt
) {
}
