package com.mentify.ai.service;

import com.mentify.ai.dto.response.LessonSummaryResponse;

import java.util.UUID;

public interface LessonSummarizationService {
    LessonSummaryResponse summarizeLesson(UUID lessonId, String authorizationHeader);
}
