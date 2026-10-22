package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminAnalyticsOverviewResponse(
        UserAnalyticsOverview users,
        CourseAnalyticsOverview courses,
        EnrollmentAnalyticsOverview enrollment,
        PaymentAnalyticsOverview payments,
        boolean partial,
        List<String> unavailableSections
) {
}
