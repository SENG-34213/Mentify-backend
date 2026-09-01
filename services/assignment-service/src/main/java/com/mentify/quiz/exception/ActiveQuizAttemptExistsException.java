package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class ActiveQuizAttemptExistsException extends QuizApiException {

    public ActiveQuizAttemptExistsException() {
        super(HttpStatus.CONFLICT, "Student already has an active attempt for this quiz");
    }
}
