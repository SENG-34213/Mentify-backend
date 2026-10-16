package com.mentify.quiz.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CourseNotFoundException extends QuizApiException {

    public CourseNotFoundException(UUID courseId) {
        super(HttpStatus.NOT_FOUND, "Course not found with id: '" + courseId + "'");
    }
}
