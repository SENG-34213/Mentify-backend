package com.mentify.quiz.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class QuizApiException extends RuntimeException {

    private final HttpStatus status;

    protected QuizApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
