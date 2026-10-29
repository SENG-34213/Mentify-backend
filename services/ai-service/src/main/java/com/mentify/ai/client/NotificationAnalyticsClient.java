package com.mentify.ai.client;

import com.mentify.ai.dto.tool.NotificationAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", contextId = "aiNotificationAnalyticsClient",
        path = "/api/v1/internal/analytics/notifications")
public interface NotificationAnalyticsClient {

    @GetMapping("/overview")
    ApiResponse<NotificationAnalyticsOverview> getOverview(
            @RequestHeader("Authorization") String authorizationHeader);
}
