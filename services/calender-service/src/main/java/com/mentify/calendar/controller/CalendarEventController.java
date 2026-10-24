package com.mentify.calendar.controller;

import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.service.CalendarEventService;
import com.mentify.common.notification.NotifyOnSuccess;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "CALENDAR_EVENT_CREATED", title = "Calendar event created", message = "Your calendar event was created successfully.")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> create(
            @Valid @RequestBody CalendarEventRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.CREATED, "Calendar event created successfully",
                calendarEventService.create(request, authorizationHeader));
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> get(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Calendar event fetched successfully",
                calendarEventService.get(eventId, authorizationHeader));
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "CALENDAR_EVENT_UPDATED", title = "Calendar event updated", message = "Your calendar event was updated successfully.")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> update(
            @PathVariable UUID eventId,
            @Valid @RequestBody CalendarEventRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Calendar event updated successfully",
                calendarEventService.update(eventId, request, authorizationHeader));
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "CALENDAR_EVENT_CANCELLED", title = "Calendar event cancelled", message = "Your calendar event was cancelled successfully.")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> cancel(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Calendar event cancelled successfully",
                calendarEventService.cancel(eventId, authorizationHeader));
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID eventId,
            @RequestHeader("Authorization") String authorizationHeader) {
        calendarEventService.delete(eventId, authorizationHeader);
        return respond(HttpStatus.OK, "Calendar event deleted successfully", null);
    }

    private <T> ResponseEntity<ApiResponse<T>> respond(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.success(status.value(), message, data));
    }
}
