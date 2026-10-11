package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarQueryService {

    List<CalendarEventResponse> findEvents(LocalDateTime start, LocalDateTime end,
                                           CalendarEventType type, UUID courseId,
                                           CalendarEventStatus status, String authorizationHeader);

    List<CalendarEventResponse> findUpcoming(Integer limit, String authorizationHeader);
}
