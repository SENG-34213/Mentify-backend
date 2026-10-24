package com.mentify.ai.dto.tool;

public record StudentAttendanceSummary(
        long completedSessions,
        long presentCount,
        long lateCount,
        long absentCount,
        long attendedCount,
        double attendancePercentage) {
}
