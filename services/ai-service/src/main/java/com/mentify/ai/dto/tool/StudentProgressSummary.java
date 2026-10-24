package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentProgressSummary(
        UUID studentId,
        int courseCount,
        int completedLessons,
        int totalLessons,
        BigDecimal lessonProgressPercentage,
        BigDecimal attendancePercentage,
        int assignmentsTotal,
        int assignmentsSubmitted,
        int assignmentsMissing,
        BigDecimal assignmentAverageScore,
        int quizzesTotal,
        int quizzesAttempted,
        BigDecimal quizAverageScore) {
}
