package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class CourseProgressResponse {
    UUID courseId;
    int completedLessons;
    int totalLessons;
    int progressPercentage;
    UUID lastAccessedLessonId;
    LocalDateTime lastAccessedAt;
}
