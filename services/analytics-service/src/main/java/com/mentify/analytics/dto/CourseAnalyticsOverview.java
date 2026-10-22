package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseAnalyticsOverview(
        long totalCourses,
        long activeCourses,
        long newThisMonth
) {
}
