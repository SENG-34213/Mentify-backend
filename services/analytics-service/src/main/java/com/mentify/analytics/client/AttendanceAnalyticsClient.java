package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.AttendanceAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "attendance-service", contextId = "analyticsAttendanceClient",
        configuration = FeignAuthorizationConfig.class)
public interface AttendanceAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/attendance/overview")
    ApiResponse<AttendanceAnalyticsSummary> getOverview();

    @GetMapping("/api/v1/internal/analytics/attendance/courses/{courseId}")
    ApiResponse<AttendanceAnalyticsSummary> getCourseSummary(@PathVariable UUID courseId);
}
