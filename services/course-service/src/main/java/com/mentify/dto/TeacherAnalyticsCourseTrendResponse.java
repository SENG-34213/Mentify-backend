package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class TeacherAnalyticsCourseTrendResponse {
    UUID courseId;
    String courseName;
    BigDecimal attendanceRate;
    BigDecimal assignmentAverage;
    BigDecimal quizAverage;
    BigDecimal examAverage;
}
