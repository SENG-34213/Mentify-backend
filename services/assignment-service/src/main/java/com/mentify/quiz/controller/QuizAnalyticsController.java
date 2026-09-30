package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.service.QuizAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/quizzes/analytics")
@RequiredArgsConstructor
public class QuizAnalyticsController {

    private final QuizAnalyticsService quizAnalyticsService;

    @GetMapping("/today/performance")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TodayQuizPerformanceResponse>> getTodayQuizPerformance() {
        TodayQuizPerformanceResponse response = quizAnalyticsService.getTodayQuizPerformance();
        return ResponseEntity.ok(ApiResponse.<TodayQuizPerformanceResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Today's quiz performance fetched successfully")
                .data(response)
                .build());
    }
}
