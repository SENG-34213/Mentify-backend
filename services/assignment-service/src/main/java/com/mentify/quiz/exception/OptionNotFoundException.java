package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OptionNotFoundException extends QuizApiException {

    public OptionNotFoundException(UUID optionId) {
        super(HttpStatus.NOT_FOUND, "Option not found with id: '" + optionId + "'");
    }
}
