package com.mentify.ai.client;

import com.mentify.ai.dto.tool.CalendarEventSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "calender-service", path = "/api/v1/calendar")
public interface CalendarEventsClient {

    @GetMapping("/upcoming")
    ApiResponse<List<CalendarEventSummary>> getUpcomingEvents(
            @RequestParam("limit") Integer limit,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
