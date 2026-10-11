package com.mentify.calendar.controller;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.CourseCalendarProjectionRequest;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.security.InternalServiceAuthorization;
import com.mentify.calendar.service.CalendarCourseProjectionService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/calendar")
@RequiredArgsConstructor
public class CalendarAssignmentQuizProjectionController {

    private final CalendarCourseProjectionService projectionService;
    private final InternalServiceAuthorization serviceAuthorization;

    @PostMapping("/assignment-projections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> upsertAssignment(
            @Valid @RequestBody CourseCalendarProjectionRequest request,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAssignmentService(serviceName, serviceToken);
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment calendar projection upserted successfully",
                projectionService.upsert(CalendarSourceType.ASSIGNMENT, CalendarEventType.ASSIGNMENT, request)));
    }

    @PostMapping("/assignment-projections/{sourceId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> cancelAssignment(
            @PathVariable UUID sourceId,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAssignmentService(serviceName, serviceToken);
        projectionService.cancel(CalendarSourceType.ASSIGNMENT, sourceId);
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment calendar projection cancellation accepted", null));
    }

    @PostMapping("/quiz-projections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> upsertQuiz(
            @Valid @RequestBody CourseCalendarProjectionRequest request,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAssignmentService(serviceName, serviceToken);
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz calendar projection upserted successfully",
                projectionService.upsert(CalendarSourceType.QUIZ, CalendarEventType.QUIZ, request)));
    }

    @PostMapping("/quiz-projections/{sourceId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> cancelQuiz(
            @PathVariable UUID sourceId,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAssignmentService(serviceName, serviceToken);
        projectionService.cancel(CalendarSourceType.QUIZ, sourceId);
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz calendar projection cancellation accepted", null));
    }

    @PostMapping("/class-projections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> upsertClass(
            @Valid @RequestBody CourseCalendarProjectionRequest request,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAttendanceService(serviceName, serviceToken);
        return ResponseEntity.ok(ApiResponse.success(200, "Class calendar projection upserted successfully",
                projectionService.upsert(CalendarSourceType.COURSE_CLASS, CalendarEventType.CLASS, request)));
    }

    @PostMapping("/class-projections/{sourceId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> cancelClass(
            @PathVariable UUID sourceId,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireAttendanceService(serviceName, serviceToken);
        projectionService.cancel(CalendarSourceType.COURSE_CLASS, sourceId);
        return ResponseEntity.ok(ApiResponse.success(200, "Class calendar projection cancellation accepted", null));
    }
}
