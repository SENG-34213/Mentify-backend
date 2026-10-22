package com.mentify.exam.service;

import com.mentify.exam.dto.response.ExamAnalyticsSummaryResponse;

import java.util.UUID;

public interface ExamAnalyticsService {

    ExamAnalyticsSummaryResponse getOverview();

    ExamAnalyticsSummaryResponse getCourseSummary(UUID courseId);
}
