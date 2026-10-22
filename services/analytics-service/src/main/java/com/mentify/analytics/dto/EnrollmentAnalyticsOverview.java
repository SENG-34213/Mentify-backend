package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentAnalyticsOverview(
        long activeEnrollments,
        long uniqueStudents,
        long newThisMonth
) {
}
