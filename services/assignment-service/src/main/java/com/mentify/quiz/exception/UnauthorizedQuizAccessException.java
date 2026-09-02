package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedQuizAccessException extends QuizApiException {

    public UnauthorizedQuizAccessException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
