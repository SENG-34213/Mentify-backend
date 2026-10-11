package com.mentify.calendar.controller;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.service.CalendarQueryService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarQueryController {

    private final CalendarQueryService queryService;

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> findEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(name = "type", required = false) CalendarEventType type,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) CalendarEventStatus status,
            @RequestHeader("Authorization") String authorizationHeader) {
        List<CalendarEventResponse> data = queryService.findEvents(start, end, type, courseId, status, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(200, "Calendar events fetched successfully", data));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> findUpcoming(
            @RequestParam(required = false) Integer limit,
            @RequestHeader("Authorization") String authorizationHeader) {
        List<CalendarEventResponse> data = queryService.findUpcoming(limit, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(200, "Upcoming calendar events fetched successfully", data));
    }
}
