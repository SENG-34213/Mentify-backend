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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/analytics/quizzes")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
public class QuizAnalyticsContractController {

    private final QuizAnalyticsService quizAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuizAnalyticsSummaryResponse>> getOverview(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz analytics fetched successfully",
                quizAnalyticsService.getOverview(from, to)));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<QuizAnalyticsSummaryResponse>> getCourseSummary(
            @PathVariable UUID courseId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Quiz analytics fetched successfully",
                quizAnalyticsService.getCourseSummary(courseId, from, to, authorizationHeader)));
    }
}
