package com.mentify.calendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/** Supplies the configured timezone to service code and JPA callbacks. */
@Component
public class CalendarTimeZone {

    private static volatile ZoneId current = ZoneId.of("Asia/Colombo");

    public CalendarTimeZone(@Value("${calendar.timezone:Asia/Colombo}") String configuredTimeZone) {
        current = ZoneId.of(configuredTimeZone);
    }

    public static ZoneId current() {
        return current;
    }
}
