package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TeacherAnalyticsMetricResponse {
    long total;
    long graded;
    long participants;
    long passed;
    BigDecimal averagePercentage;
    BigDecimal attendanceRate;
}
