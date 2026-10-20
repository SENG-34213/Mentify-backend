package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TeacherAttendanceCorrelationResponse {
    String attendanceRange;
    BigDecimal averageAssessmentPercentage;
    long studentCount;
}
