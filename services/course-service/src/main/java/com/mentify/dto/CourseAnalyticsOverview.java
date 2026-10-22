package com.mentify.dto;

public record CourseAnalyticsOverview(
        long totalCourses,
        long activeCourses,
        long newThisMonth
) {
}
