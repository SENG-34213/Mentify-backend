package com.mentify.controller;

import com.mentify.dto.EnrollmentAnalyticsOverviewResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.EntrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/analytics/enrollments")
@RequiredArgsConstructor
public class EnrollmentAnalyticsController {

    private final EntrollmentService entrollmentService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<EnrollmentAnalyticsOverviewResponse>> getOverview() {
        ApiResponse<EnrollmentAnalyticsOverviewResponse> response = entrollmentService.getAnalyticsOverview();
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<EnrollmentAnalyticsOverviewResponse>> getCourseSummary(
            @PathVariable UUID courseId
    ) {
        ApiResponse<EnrollmentAnalyticsOverviewResponse> response = entrollmentService.getAnalyticsCourseSummary(courseId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
