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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/analytics/assignments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class AssignmentAnalyticsController {

    private final AssignmentAnalyticsService assignmentAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AssignmentAnalyticsSummaryResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment analytics fetched successfully",
                assignmentAnalyticsService.getOverview()));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<AssignmentAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(200, "Assignment analytics fetched successfully",
                assignmentAnalyticsService.getCourseSummary(courseId)));
    }
}
