package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class QuizNotPublishedException extends QuizApiException {

    public QuizNotPublishedException() {
        super(HttpStatus.BAD_REQUEST, "Only published quizzes can be attempted");
    }
}
