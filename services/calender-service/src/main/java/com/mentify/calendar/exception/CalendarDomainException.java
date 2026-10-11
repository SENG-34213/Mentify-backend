package com.mentify.calendar.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CalendarDomainException extends RuntimeException {
    private final HttpStatus status;

    public CalendarDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
