package com.mentify.ai.client;

import com.mentify.ai.dto.tool.AssignmentAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "aiAssignmentAnalyticsClient", path = "/api/v1/internal/analytics/assignments")
public interface AssignmentAnalyticsClient {

    @GetMapping("/overview")
    ApiResponse<AssignmentAnalyticsSummary> getOverview(
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/courses/{courseId}")
    ApiResponse<AssignmentAnalyticsSummary> getCourseSummary(
            @PathVariable("courseId") UUID courseId,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader);
}
