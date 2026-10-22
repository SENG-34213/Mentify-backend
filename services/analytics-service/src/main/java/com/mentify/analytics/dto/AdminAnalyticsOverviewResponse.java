package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminAnalyticsOverviewResponse(
        UserAnalyticsOverview users,
        CourseAnalyticsOverview courses,
        EnrollmentAnalyticsOverview enrollment,
        PaymentAnalyticsOverview payments,
        AttendanceAnalyticsSummary attendance,
        AssessmentAnalyticsSummary assignments,
        AssessmentAnalyticsSummary quizzes,
        AssessmentAnalyticsSummary exams,
        boolean partial,
        List<String> unavailableSections
) {
}
