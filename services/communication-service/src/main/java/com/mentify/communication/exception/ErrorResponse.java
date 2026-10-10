package com.mentify.communication.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
        int statusCode,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int statusCode, String message, String path) {
        return new ErrorResponse(statusCode, message, path, LocalDateTime.now());
    }
}
