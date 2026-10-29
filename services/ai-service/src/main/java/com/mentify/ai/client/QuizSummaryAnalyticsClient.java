package com.mentify.ai.client;

import com.mentify.ai.dto.tool.QuizAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "aiQuizSummaryAnalyticsClient", path = "/api/v1/internal/analytics/quizzes")
public interface QuizSummaryAnalyticsClient {

    @GetMapping("/overview")
    ApiResponse<QuizAnalyticsSummary> getOverview(
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/courses/{courseId}")
    ApiResponse<QuizAnalyticsSummary> getCourseSummary(
            @PathVariable("courseId") UUID courseId,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader);
}
