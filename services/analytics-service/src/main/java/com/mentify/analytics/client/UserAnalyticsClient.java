package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.UserAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-service", contextId = "analyticsUserClient",
        configuration = FeignAuthorizationConfig.class)
public interface UserAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/users/overview")
    ApiResponse<UserAnalyticsOverview> getOverview();
}
