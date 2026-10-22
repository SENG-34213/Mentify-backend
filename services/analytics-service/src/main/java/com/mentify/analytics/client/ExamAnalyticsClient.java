package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.AssessmentAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "exam-service", contextId = "analyticsExamClient",
        configuration = FeignAuthorizationConfig.class)
public interface ExamAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/exams/overview")
    ApiResponse<AssessmentAnalyticsSummary> getOverview();

    @GetMapping("/api/v1/internal/analytics/exams/courses/{courseId}")
    ApiResponse<AssessmentAnalyticsSummary> getCourseSummary(@PathVariable UUID courseId);
}
