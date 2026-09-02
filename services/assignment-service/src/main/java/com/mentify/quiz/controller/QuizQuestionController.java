package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.dto.response.TeacherQuestionResponse;
import com.mentify.quiz.service.QuizQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes/{quizId}/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class QuizQuestionController {

    private final QuizQuestionService quizQuestionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherQuestionResponse>> createQuestion(
            @PathVariable UUID quizId,
            @Valid @RequestBody CreateQuestionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<TeacherQuestionResponse> response =
                quizQuestionService.createQuestion(quizId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<TeacherQuestionResponse>> updateQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateQuestionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<TeacherQuestionResponse> response =
                quizQuestionService.updateQuestion(quizId, questionId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Object>> deleteQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<Object> response = quizQuestionService.deleteQuestion(quizId, questionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
