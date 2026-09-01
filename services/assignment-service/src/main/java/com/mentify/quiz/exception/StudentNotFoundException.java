package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class StudentNotFoundException extends QuizApiException {

    public StudentNotFoundException(UUID studentId) {
        super(HttpStatus.NOT_FOUND, "Student not found with id: '" + studentId + "'");
    }
}
