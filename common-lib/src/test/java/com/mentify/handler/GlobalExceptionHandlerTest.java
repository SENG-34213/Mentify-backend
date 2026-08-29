package com.mentify.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnFriendlyMessageForInvalidUuidInBody() {
        InvalidFormatException invalidFormatException = InvalidFormatException.from(
                null,
                "Invalid UUID",
                "not-a-uuid",
                UUID.class
        );
        invalidFormatException.prependPath(new Object(), 0);
        invalidFormatException.prependPath(new Object(), "courseIds");

        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", invalidFormatException, null);

        ResponseEntity<ApiResponse<Object>> response = handler.handleHttpMessageNotReadableException(ex, mock(WebRequest.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid UUID format for field 'courseIds[0]'", response.getBody().getMessage());
    }

    @Test
    void shouldReturnGenericMalformedJsonMessageForOtherParseErrors() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", new RuntimeException("oops"), null);

        ResponseEntity<ApiResponse<Object>> response = handler.handleHttpMessageNotReadableException(ex, mock(WebRequest.class));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().getMessage());
    }
}

