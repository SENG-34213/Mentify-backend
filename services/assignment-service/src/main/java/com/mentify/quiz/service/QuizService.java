package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.CreateQuizRequest;
import com.mentify.quiz.dto.request.UpdateQuizRequest;
import com.mentify.quiz.dto.response.QuizResponse;
import com.mentify.quiz.dto.response.StudentQuizResponse;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    ApiResponse<QuizResponse> createQuiz(CreateQuizRequest request, String authorizationHeader);

    ApiResponse<QuizResponse> updateQuiz(UUID quizId, UpdateQuizRequest request, String authorizationHeader);

    ApiResponse<QuizResponse> publishQuiz(UUID quizId, String authorizationHeader);

    ApiResponse<QuizResponse> getTeacherQuiz(UUID quizId);

    ApiResponse<List<StudentQuizResponse>> getPublishedQuizzesByCourse(UUID courseId, String authorizationHeader);

    ApiResponse<StudentQuizResponse> getStudentQuiz(UUID quizId, String authorizationHeader);
}
