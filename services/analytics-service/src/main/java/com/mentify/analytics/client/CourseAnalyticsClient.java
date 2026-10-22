package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.CourseAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "course-service", contextId = "analyticsCourseClient",
        configuration = FeignAuthorizationConfig.class)
public interface CourseAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/courses/overview")
    ApiResponse<CourseAnalyticsOverview> getOverview();
}
