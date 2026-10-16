package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

public class StudentNotEnrolledException extends QuizApiException {

    public StudentNotEnrolledException() {
        super(HttpStatus.FORBIDDEN, "Student is not enrolled in this course");
    }
}
