package com.mentify.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private UUID studentId;
    private UUID courseId;
    private long completedSessions;
    private long presentCount;
    private long lateCount;
    private long absentCount;
    private long attendedCount;
    private double attendancePercentage;
}
