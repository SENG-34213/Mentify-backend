package com.mentify.attendance.client;

import com.mentify.attendance.client.dto.CalendarProjectionRequest;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "calender-service", url = "${mentify.clients.calendar-service.url:}",
        path = "/api/v1/internal/calendar")
public interface CalendarProjectionClient {

    @PostMapping("/attendance-projections")
    ApiResponse<Object> upsert(@RequestBody CalendarProjectionRequest request,
                               @RequestHeader("Authorization") String authorizationHeader,
                               @RequestHeader("X-Internal-Service-Name") String serviceName,
                               @RequestHeader("X-Internal-Service-Token") String serviceToken);

    @PostMapping("/attendance-projections/{sourceId}/cancel")
    ApiResponse<Void> cancel(@PathVariable UUID sourceId,
                             @RequestHeader("Authorization") String authorizationHeader,
                             @RequestHeader("X-Internal-Service-Name") String serviceName,
                             @RequestHeader("X-Internal-Service-Token") String serviceToken);
}
