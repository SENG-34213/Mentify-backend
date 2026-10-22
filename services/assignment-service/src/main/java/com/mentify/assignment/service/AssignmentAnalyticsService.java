package com.mentify.assignment.service;

import com.mentify.assignment.dto.response.AssignmentAnalyticsSummaryResponse;

import java.util.UUID;

public interface AssignmentAnalyticsService {

    AssignmentAnalyticsSummaryResponse getOverview();

    AssignmentAnalyticsSummaryResponse getCourseSummary(UUID courseId);
}
