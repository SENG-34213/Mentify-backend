package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.EnrollmentAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "entrollment-service", contextId = "analyticsEnrollmentClient",
        configuration = FeignAuthorizationConfig.class)
public interface EnrollmentAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/enrollments/overview")
    ApiResponse<EnrollmentAnalyticsOverview> getOverview();

    @GetMapping("/api/v1/internal/analytics/enrollments/courses/{courseId}")
    ApiResponse<EnrollmentAnalyticsOverview> getCourseSummary(@PathVariable UUID courseId);
}
