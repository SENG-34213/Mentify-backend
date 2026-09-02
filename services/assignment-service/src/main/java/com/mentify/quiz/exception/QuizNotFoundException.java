package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuizNotFoundException extends QuizApiException {

    public QuizNotFoundException(UUID quizId) {
        super(HttpStatus.NOT_FOUND, "Quiz not found with id: '" + quizId + "'");
    }
}
