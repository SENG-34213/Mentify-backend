package com.mentify.dto;

import java.util.List;
import java.util.Map;

public record EnrollmentAnalyticsOverviewResponse(
        long activeEnrollments,
        long uniqueStudents,
        long newThisMonth,
        List<CoursePopularityResponse> coursePopularity,
        Map<String, Long> enrollmentTrend
) {
}
