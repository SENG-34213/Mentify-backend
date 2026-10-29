package com.mentify.ai.dto.tool;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentCourseProgress(
        UUID courseId,
        int completedLessons,
        int totalLessons,
        int progressPercentage,
        UUID lastAccessedLessonId,
        LocalDateTime lastAccessedAt) {
}
