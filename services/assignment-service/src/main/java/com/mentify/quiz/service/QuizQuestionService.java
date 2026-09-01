package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.dto.response.TeacherQuestionResponse;

import java.util.UUID;

public interface QuizQuestionService {

    ApiResponse<TeacherQuestionResponse> createQuestion(UUID quizId, CreateQuestionRequest request, String authorizationHeader);

    ApiResponse<TeacherQuestionResponse> updateQuestion(
            UUID quizId,
            UUID questionId,
            CreateQuestionRequest request,
            String authorizationHeader
    );

    ApiResponse<Object> deleteQuestion(UUID quizId, UUID questionId, String authorizationHeader);
}
