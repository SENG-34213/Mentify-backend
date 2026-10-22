package com.mentify.quiz.service;

import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.dto.response.QuizAnalyticsSummaryResponse;

import java.util.UUID;

public interface QuizAnalyticsService {

    TodayQuizPerformanceResponse getTodayQuizPerformance();

    QuizAnalyticsSummaryResponse getOverview();

    QuizAnalyticsSummaryResponse getCourseSummary(UUID courseId);
}
