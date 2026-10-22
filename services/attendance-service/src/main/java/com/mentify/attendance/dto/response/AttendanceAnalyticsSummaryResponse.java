package com.mentify.attendance.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AttendanceAnalyticsSummaryResponse(
        UUID courseId,
        BigDecimal averageAttendance
) {
}
