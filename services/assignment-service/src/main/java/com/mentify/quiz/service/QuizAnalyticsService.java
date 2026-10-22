package com.mentify.quiz.service;

import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.dto.response.QuizAnalyticsSummaryResponse;

import java.util.UUID;
import java.time.LocalDate;

public interface QuizAnalyticsService {

    TodayQuizPerformanceResponse getTodayQuizPerformance();

    QuizAnalyticsSummaryResponse getOverview();

    QuizAnalyticsSummaryResponse getCourseSummary(UUID courseId);

    QuizAnalyticsSummaryResponse getOverview(LocalDate from, LocalDate to);

    QuizAnalyticsSummaryResponse getCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader);
}
