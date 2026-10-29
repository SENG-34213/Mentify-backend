package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.util.UUID;

public record AttendanceAnalyticsSummary(
        UUID courseId,
        BigDecimal averageAttendance
) {
}
