package com.mentify.ai.client;

import com.mentify.ai.dto.tool.UserRegistrationOverviewToolResult;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/api/internal/users/analytics")
public interface UserAnalyticsClient {

    @GetMapping("/registration-overview")
    ApiResponse<UserRegistrationOverviewToolResult> getRegistrationOverview(
            @RequestHeader("Authorization") String authorizationHeader
    );
}
