package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;

import java.util.UUID;

public interface CalendarEventService {
    CalendarEventResponse create(CalendarEventRequest request, String authorizationHeader);
    CalendarEventResponse get(UUID eventId, String authorizationHeader);
    CalendarEventResponse update(UUID eventId, CalendarEventRequest request, String authorizationHeader);
    CalendarEventResponse cancel(UUID eventId, String authorizationHeader);
    void delete(UUID eventId, String authorizationHeader);
}
