package com.mentify.exam.exception;

import org.springframework.http.HttpStatus;

/** Base type for Exam Service domain errors that are safe to expose to API clients. */
public class ExamDomainException extends RuntimeException {

    private final HttpStatus status;

    public ExamDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
