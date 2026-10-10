package com.mentify.exception;

import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(PaymentDomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomain(PaymentDomainException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ApiResponse<Object>> handleProvider(PaymentProviderException ex) {
        log.warn("Payment provider operation failed: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Payment provider is currently unavailable. Please try again later.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Payment data integrity violation: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing payment data");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleDependencyFailure(FeignException ex) {
        log.warn("Payment dependency call failed with status {}", ex.status());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A dependent service is currently unavailable. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception in payment-service", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST, message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .statusCode(status.value())
                .status(status)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Object> body(HttpStatus status, String message) {
        return (ResponseEntity<Object>) (ResponseEntity<?>) build(status, message);
    }
}
