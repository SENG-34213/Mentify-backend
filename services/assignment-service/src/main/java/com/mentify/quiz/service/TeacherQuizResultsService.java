package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.TeacherQuizResultsResponse;

import java.util.UUID;

public interface TeacherQuizResultsService {
    ApiResponse<TeacherQuizResultsResponse> getResults(UUID quizId, String authorizationHeader);
}
