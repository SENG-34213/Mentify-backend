package com.mentify.client;

import com.mentify.client.dto.AttendanceSessionAnalyticsResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "attendance-service", path = "/api/v1/attendance")
public interface AttendanceAnalyticsClient {
    @GetMapping("/courses/{courseId}/sessions")
    ApiResponse<List<AttendanceSessionAnalyticsResponse>> getSessions(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
