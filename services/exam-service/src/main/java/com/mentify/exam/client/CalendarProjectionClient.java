package com.mentify.exam.client;

import com.mentify.exam.client.dto.ExamCalendarProjectionRequest;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "calender-service", url = "${mentify.clients.calendar-service.url:}",
        path = "/api/v1/internal/calendar/exam-projections")
public interface CalendarProjectionClient {

    @PostMapping
    ApiResponse<Object> upsert(
            @RequestBody ExamCalendarProjectionRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken);

    @PostMapping("/{examId}/cancel")
    ApiResponse<Void> cancel(
            @PathVariable UUID examId,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken);
}
