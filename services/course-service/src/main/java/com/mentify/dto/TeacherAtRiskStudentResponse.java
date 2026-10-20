package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class TeacherAtRiskStudentResponse {
    UUID studentId;
    String studentName;
    BigDecimal attendancePercentage;
    BigDecimal averagePercentage;
    String reason;
}
