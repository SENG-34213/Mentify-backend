package com.mentify.attendance.controller;

import com.mentify.attendance.dto.response.AttendanceAnalyticsSummaryResponse;
import com.mentify.attendance.service.AttendanceSummaryService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/analytics/attendance")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class AttendanceAnalyticsController {

    private final AttendanceSummaryService attendanceSummaryService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AttendanceAnalyticsSummaryResponse>> getOverview(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(attendanceSummaryService.getAnalyticsOverview(from, to));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<AttendanceAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(attendanceSummaryService.getAnalyticsCourseSummary(
                courseId, from, to, authorizationHeader));
    }
}
