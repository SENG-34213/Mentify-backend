package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.QuizAnalyticsSummaryResponse;
import com.mentify.quiz.service.QuizAnalyticsService;
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
@RequestMapping("/api/v1/internal/analytics/quizzes")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class QuizAnalyticsContractController {

    private final QuizAnalyticsService quizAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<QuizAnalyticsSummaryResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz analytics fetched successfully",
                quizAnalyticsService.getOverview()));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<QuizAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz analytics fetched successfully",
                quizAnalyticsService.getCourseSummary(courseId)));
    }
}
