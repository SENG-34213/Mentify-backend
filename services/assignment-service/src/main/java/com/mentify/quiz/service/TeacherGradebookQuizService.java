package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.TeacherGradebookQuizResponse;

import java.util.List;
import java.util.UUID;

public interface TeacherGradebookQuizService {
    ApiResponse<List<TeacherGradebookQuizResponse>> getQuizzes(UUID courseId, String authorizationHeader);
}
