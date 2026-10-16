package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.SaveStudentAnswerRequest;
import com.mentify.quiz.dto.response.StartQuizAttemptResponse;
import com.mentify.quiz.dto.response.StudentAnswerResponse;
import com.mentify.quiz.dto.response.SubmitQuizResponse;

import java.util.UUID;

public interface QuizAttemptService {

    ApiResponse<StartQuizAttemptResponse> startAttempt(UUID quizId, String authorizationHeader);

    ApiResponse<StudentAnswerResponse> saveAnswer(UUID attemptId, SaveStudentAnswerRequest request);

    ApiResponse<SubmitQuizResponse> submitAttempt(UUID attemptId);
}
