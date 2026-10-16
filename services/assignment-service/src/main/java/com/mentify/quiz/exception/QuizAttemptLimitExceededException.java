package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class QuizAttemptLimitExceededException extends QuizApiException {

    public QuizAttemptLimitExceededException() {
        super(HttpStatus.CONFLICT, "Quiz attempt limit has been exceeded");
    }
}
