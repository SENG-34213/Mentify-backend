package com.mentify.ai.client;

import com.mentify.ai.dto.tool.AttendanceAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.time.LocalDate;

@FeignClient(name = "attendance-service", contextId = "aiAttendanceAnalyticsClient", path = "/api/v1/internal/analytics/attendance")
public interface AttendanceAnalyticsClient {

    @GetMapping("/overview")
    ApiResponse<AttendanceAnalyticsSummary> getOverview(
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/courses/{courseId}")
    ApiResponse<AttendanceAnalyticsSummary> getCourseSummary(
            @PathVariable("courseId") UUID courseId,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
