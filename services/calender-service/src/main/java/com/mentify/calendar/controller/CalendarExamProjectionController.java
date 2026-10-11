package com.mentify.calendar.controller;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.ExamCalendarProjectionRequest;
import com.mentify.calendar.security.InternalServiceAuthorization;
import com.mentify.calendar.service.CalendarExamProjectionService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/calendar/exam-projections")
@RequiredArgsConstructor
public class CalendarExamProjectionController {

    private final CalendarExamProjectionService projectionService;
    private final InternalServiceAuthorization serviceAuthorization;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> upsert(
            @Valid @org.springframework.web.bind.annotation.RequestBody ExamCalendarProjectionRequest request,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireExamService(serviceName, serviceToken);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Exam calendar projection upserted successfully", projectionService.upsert(request)));
    }

    @PostMapping("/{examId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID examId,
            @RequestHeader("X-Internal-Service-Name") String serviceName,
            @RequestHeader("X-Internal-Service-Token") String serviceToken) {
        serviceAuthorization.requireExamService(serviceName, serviceToken);
        projectionService.cancel(examId);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Exam calendar projection cancellation accepted", null));
    }
}
