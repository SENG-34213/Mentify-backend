package com.mentify.material.exception;

import com.mentify.payload.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaterialExceptionHandler {

    static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(MaterialValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MaterialValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaterialStorageException.class)
    public ResponseEntity<ApiResponse<Object>> handleStorage(MaterialStorageException ex) {
        log.warn("Material storage operation failed: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MaterialNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(MaterialNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MaterialUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnavailable(MaterialUnavailableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation in material-service: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing material data");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required multipart file or field is missing");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception in material-service", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .statusCode(status.value())
                .status(status)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
