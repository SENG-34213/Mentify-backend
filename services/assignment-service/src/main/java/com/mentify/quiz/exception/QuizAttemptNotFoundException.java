package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuizAttemptNotFoundException extends QuizApiException {

    public QuizAttemptNotFoundException(UUID attemptId) {
        super(HttpStatus.NOT_FOUND, "Quiz attempt not found with id: '" + attemptId + "'");
    }
}
