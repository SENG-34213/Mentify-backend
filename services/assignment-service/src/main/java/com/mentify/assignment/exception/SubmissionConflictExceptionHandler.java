package com.mentify.assignment.exception;

import com.mentify.payload.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubmissionConflictExceptionHandler {

    @ExceptionHandler(SubmissionConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleSubmissionConflict(SubmissionConflictException exception) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(exception.getMessage())
                .statusCode(HttpStatus.CONFLICT.value())
                .status(HttpStatus.CONFLICT)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
