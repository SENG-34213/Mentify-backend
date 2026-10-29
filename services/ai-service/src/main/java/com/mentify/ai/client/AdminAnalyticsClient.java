package com.mentify.ai.client;

import com.mentify.ai.dto.tool.AdminDashboardOverview;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "analytics-service", contextId = "aiAdminAnalyticsClient",
        path = "/api/v1/analytics/admin")
public interface AdminAnalyticsClient {

    @GetMapping("/overview")
    AdminDashboardOverview getOverview(
            @RequestHeader("Authorization") String authorizationHeader);
}
