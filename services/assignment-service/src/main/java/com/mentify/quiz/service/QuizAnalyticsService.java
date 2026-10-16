package com.mentify.quiz.service;

import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;

public interface QuizAnalyticsService {

    TodayQuizPerformanceResponse getTodayQuizPerformance();
}
