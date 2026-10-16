package com.mentify.ai.client;

import com.mentify.ai.dto.tool.TodayQuizPerformanceToolResult;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "assignment-service", path = "/api/internal/quizzes/analytics")
public interface QuizAnalyticsClient {

    @GetMapping("/today/performance")
    ApiResponse<TodayQuizPerformanceToolResult> getTodayQuizPerformance(
            @RequestHeader("Authorization") String authorizationHeader
    );
}
