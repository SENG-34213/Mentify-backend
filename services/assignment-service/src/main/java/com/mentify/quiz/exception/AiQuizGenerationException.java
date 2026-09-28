package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class AiQuizGenerationException extends QuizApiException {

    public AiQuizGenerationException(HttpStatus status, String message) {
        super(status, message);
    }
}
