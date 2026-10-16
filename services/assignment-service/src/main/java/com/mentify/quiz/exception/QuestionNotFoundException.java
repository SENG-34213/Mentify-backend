package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuestionNotFoundException extends QuizApiException {

    public QuestionNotFoundException(UUID questionId) {
        super(HttpStatus.NOT_FOUND, "Question not found with id: '" + questionId + "'");
    }
}
