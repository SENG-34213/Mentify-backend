package com.mentify.assignment.service;

import com.mentify.assignment.dto.response.AssignmentAnalyticsSummaryResponse;

import java.util.UUID;
import java.time.LocalDate;

public interface AssignmentAnalyticsService {

    AssignmentAnalyticsSummaryResponse getOverview();

    AssignmentAnalyticsSummaryResponse getCourseSummary(UUID courseId);

    AssignmentAnalyticsSummaryResponse getOverview(LocalDate from, LocalDate to);

    AssignmentAnalyticsSummaryResponse getCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader);
}
