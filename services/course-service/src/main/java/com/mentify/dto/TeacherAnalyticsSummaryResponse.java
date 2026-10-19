package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TeacherAnalyticsSummaryResponse {
    long studentCount;
    BigDecimal submissionRate;
    BigDecimal attendanceAverage;
    BigDecimal averagePercentage;
    BigDecimal gpa;
    long interventionCount;
}
