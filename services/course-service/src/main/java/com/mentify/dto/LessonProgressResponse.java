package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class LessonProgressResponse {
    UUID lessonId;
    UUID courseId;
    boolean completed;
    LocalDateTime lastAccessedAt;
}
