package com.mentify.calendar.exception;

import com.mentify.payload.response.ApiResponse;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CalendarExceptionHandler {

    @ExceptionHandler(CalendarDomainException.class)
    ResponseEntity<ApiResponse<Object>> handleDomain(CalendarDomainException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        String message = ex.getMessage();
        return build(HttpStatus.FORBIDDEN,
                message == null || message.isBlank() ? "Access denied" : message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getParameterName() + " is required");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Object>> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Object>> handleMissingRoute(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Object>> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Calendar data integrity violation: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    ResponseEntity<ApiResponse<Object>> handleConcurrentCalendarWrite(ConcurrencyFailureException ex) {
        return build(HttpStatus.CONFLICT,
                "The calendar changed while you were saving. Refresh and try another available time.");
    }

    @ExceptionHandler(FeignException.class)
    ResponseEntity<ApiResponse<Object>> handleDependency(FeignException ex) {
        log.warn("Calendar dependency call failed with status {}", ex.status());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A dependent service is currently unavailable. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled calendar-service error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .statusCode(status.value()).status(status).message(message).build();
        return ResponseEntity.status(status).body(response);
    }
}
