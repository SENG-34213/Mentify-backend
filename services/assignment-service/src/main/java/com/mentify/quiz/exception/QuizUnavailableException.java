package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class QuizUnavailableException extends QuizApiException {

    public QuizUnavailableException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
