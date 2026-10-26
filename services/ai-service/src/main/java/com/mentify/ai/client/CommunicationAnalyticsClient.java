package com.mentify.ai.client;

import com.mentify.ai.dto.tool.CommunicationAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "communication-service", contextId = "aiCommunicationAnalyticsClient",
        path = "/api/v1/internal/analytics/communication")
public interface CommunicationAnalyticsClient {

    @GetMapping("/overview")
    ApiResponse<CommunicationAnalyticsOverview> getOverview(
            @RequestHeader("Authorization") String authorizationHeader);
}
