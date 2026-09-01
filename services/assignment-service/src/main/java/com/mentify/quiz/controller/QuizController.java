package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.CreateQuizRequest;
import com.mentify.quiz.dto.request.UpdateQuizRequest;
import com.mentify.quiz.dto.response.QuizResponse;
import com.mentify.quiz.dto.response.StudentQuizResponse;
import com.mentify.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/api/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @Valid @RequestBody CreateQuizRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<QuizResponse> response = quizService.createQuiz(request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/api/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable UUID quizId,
            @Valid @RequestBody UpdateQuizRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<QuizResponse> response = quizService.updateQuiz(quizId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> getTeacherQuiz(@PathVariable UUID quizId) {
        ApiResponse<QuizResponse> response = quizService.getTeacherQuiz(quizId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/api/quizzes/{quizId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> publishQuiz(
            @PathVariable UUID quizId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<QuizResponse> response = quizService.publishQuiz(quizId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/courses/{courseId}/quizzes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentQuizResponse>>> getPublishedQuizzesByCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<StudentQuizResponse>> response =
                quizService.getPublishedQuizzesByCourse(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/quizzes/{quizId}/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentQuizResponse>> getStudentQuiz(
            @PathVariable UUID quizId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<StudentQuizResponse> response = quizService.getStudentQuiz(quizId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
