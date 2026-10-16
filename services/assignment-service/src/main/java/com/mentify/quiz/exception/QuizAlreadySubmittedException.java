package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class QuizAlreadySubmittedException extends QuizApiException {

    public QuizAlreadySubmittedException() {
        super(HttpStatus.CONFLICT, "Quiz attempt has already been submitted");
    }
}
