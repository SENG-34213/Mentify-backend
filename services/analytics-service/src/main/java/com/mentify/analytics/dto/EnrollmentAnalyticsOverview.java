package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentAnalyticsOverview(
        long activeEnrollments,
        long uniqueStudents,
        long newThisMonth,
        List<CoursePopularityAnalytics> coursePopularity,
        Map<String, Long> enrollmentTrend
) {
}
