package com.mentify.assignment.exception;

public class SubmissionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SubmissionConflictException(String message) {
        super(message);
    }
}
