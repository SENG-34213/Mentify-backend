package com.mentify.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND)
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message("Access denied")
                .status(HttpStatus.FORBIDDEN)
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");

        ApiResponse<Object> response = ApiResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.FORBIDDEN)
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {
        String message = UUID.class.equals(ex.getRequiredType())
                ? "Invalid UUID format for parameter '" + ex.getName() + "'"
                : "Invalid value for parameter '" + ex.getName() + "'";

        ApiResponse<Object> response = ApiResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex,
            WebRequest request
    ) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message("Required request header '" + ex.getHeaderName() + "' is missing")
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            WebRequest request
    ) {
        log.warn("Upload rejected because it exceeded the configured limit for {}", request.getDescription(false));
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "This file is too large. Please upload a file smaller than 100 MB.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException
                && UUID.class.equals(invalidFormatException.getTargetType())) {
            String fieldPath = invalidFormatException.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    .replace(".[", "[");

            String message = fieldPath.isBlank()
                    ? "Invalid UUID format in request body"
                    : "Invalid UUID format for field '" + fieldPath + "'";

            ApiResponse<Object> response = ApiResponse.builder()
                    .message(message)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        ApiResponse<Object> response = ApiResponse.builder()
                .message("Malformed JSON request")
                .status(HttpStatus.BAD_REQUEST)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Database connectivity and transaction failures are operational errors,
     * not client errors. Log the technical cause on the server, but never send
     * JDBC/Hibernate details to the browser.
     */
    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessException.class,
            TransactionSystemException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleDatabaseFailure(Exception ex, WebRequest request) {
        log.error("Database transaction failed for {}", request.getDescription(false), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "The service is temporarily unavailable. Please try again shortly.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled application error for {}", request.getDescription(false), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later.");
    }

    private ResponseEntity<ApiResponse<Object>> buildResponse(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message(message)
                .status(status)
                .statusCode(status.value())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
