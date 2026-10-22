package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.AssessmentAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "analyticsAssignmentClient",
        configuration = FeignAuthorizationConfig.class)
public interface AssignmentAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/assignments/overview")
    ApiResponse<AssessmentAnalyticsSummary> getOverview();

    @GetMapping("/api/v1/internal/analytics/assignments/courses/{courseId}")
    ApiResponse<AssessmentAnalyticsSummary> getCourseSummary(@PathVariable UUID courseId);
}
