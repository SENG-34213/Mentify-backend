package com.mentify.assignment.client;

import com.mentify.assignment.client.dto.CalendarProjectionRequest;
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

    @PostMapping("/assignment-projections")
    ApiResponse<Object> upsertAssignment(@RequestBody CalendarProjectionRequest request,
                                         @RequestHeader("Authorization") String authorizationHeader,
                                         @RequestHeader("X-Internal-Service-Name") String serviceName,
                                         @RequestHeader("X-Internal-Service-Token") String serviceToken);

    @PostMapping("/assignment-projections/{sourceId}/cancel")
    ApiResponse<Void> cancelAssignment(@PathVariable UUID sourceId,
                                       @RequestHeader("Authorization") String authorizationHeader,
                                       @RequestHeader("X-Internal-Service-Name") String serviceName,
                                       @RequestHeader("X-Internal-Service-Token") String serviceToken);

    @PostMapping("/quiz-projections")
    ApiResponse<Object> upsertQuiz(@RequestBody CalendarProjectionRequest request,
                                   @RequestHeader("Authorization") String authorizationHeader,
                                   @RequestHeader("X-Internal-Service-Name") String serviceName,
                                   @RequestHeader("X-Internal-Service-Token") String serviceToken);

    @PostMapping("/quiz-projections/{sourceId}/cancel")
    ApiResponse<Void> cancelQuiz(@PathVariable UUID sourceId,
                                 @RequestHeader("Authorization") String authorizationHeader,
                                 @RequestHeader("X-Internal-Service-Name") String serviceName,
                                 @RequestHeader("X-Internal-Service-Token") String serviceToken);
}
