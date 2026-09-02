package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuestionOptionsException extends QuizApiException {

    public InvalidQuestionOptionsException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
