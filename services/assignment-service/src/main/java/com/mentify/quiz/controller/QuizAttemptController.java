package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.SaveStudentAnswerRequest;
import com.mentify.quiz.dto.response.StartQuizAttemptResponse;
import com.mentify.quiz.dto.response.StudentAnswerResponse;
import com.mentify.quiz.dto.response.SubmitQuizResponse;
import com.mentify.quiz.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @PostMapping("/api/quizzes/{quizId}/attempts")
    public ResponseEntity<ApiResponse<StartQuizAttemptResponse>> startAttempt(
            @PathVariable UUID quizId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<StartQuizAttemptResponse> response = quizAttemptService.startAttempt(quizId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/api/quiz-attempts/{attemptId}/answers")
    public ResponseEntity<ApiResponse<StudentAnswerResponse>> saveAnswer(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SaveStudentAnswerRequest request
    ) {
        ApiResponse<StudentAnswerResponse> response = quizAttemptService.saveAnswer(attemptId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/api/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<SubmitQuizResponse>> submitAttempt(@PathVariable UUID attemptId) {
        ApiResponse<SubmitQuizResponse> response = quizAttemptService.submitAttempt(attemptId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
