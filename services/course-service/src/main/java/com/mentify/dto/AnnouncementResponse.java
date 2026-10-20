package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class AnnouncementResponse {
    UUID id;
    UUID courseId;
    UUID authorId;
    String title;
    String message;
    LocalDateTime publishedAt;
}
