package com.mentify.assignment.controller;

import com.mentify.assignment.dto.response.AssignmentAnalyticsSummaryResponse;
import com.mentify.assignment.service.AssignmentAnalyticsService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/analytics/assignments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class AssignmentAnalyticsController {

    private final AssignmentAnalyticsService assignmentAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentAnalyticsSummaryResponse>> getOverview(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment analytics fetched successfully",
                assignmentAnalyticsService.getOverview(from, to)));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<AssignmentAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment analytics fetched successfully",
                assignmentAnalyticsService.getCourseSummary(courseId, from, to, authorizationHeader)));
    }
}
