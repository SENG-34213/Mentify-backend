package com.mentify.exam.controller;

import com.mentify.exam.dto.response.ExamAnalyticsSummaryResponse;
import com.mentify.exam.service.ExamAnalyticsService;
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
@RequestMapping("/api/v1/internal/analytics/exams")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class ExamAnalyticsController {

    private final ExamAnalyticsService examAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ExamAnalyticsSummaryResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(200, "Exam analytics fetched successfully",
                examAnalyticsService.getOverview()));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<ExamAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(200, "Exam analytics fetched successfully",
                examAnalyticsService.getCourseSummary(courseId)));
    }
}
