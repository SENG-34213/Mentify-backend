package com.mentify.attendance.controller;

import com.mentify.attendance.service.CalendarProjectionService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/calendar-reconciliation")
@RequiredArgsConstructor
public class CalendarProjectionReconciliationController {

    private final CalendarProjectionService projectionService;

    @PostMapping("/class-sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reconcile(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorizationHeader) {
        projectionService.reconcile(sessionId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(200, "Class schedule calendar reconciliation completed", null));
    }
}
