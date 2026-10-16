package com.mentify.quiz.handler;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.exception.QuizApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class QuizExceptionHandler {

    @ExceptionHandler(QuizApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleQuizApiException(QuizApiException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(ex.getMessage())
                .statusCode(ex.getStatus().value())
                .status(ex.getStatus())
                .build();

        return new ResponseEntity<>(response, ex.getStatus());
    }
}
