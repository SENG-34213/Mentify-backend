package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.CourseCalendarProjectionRequest;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;

import java.util.UUID;

public interface CalendarCourseProjectionService {

    CalendarEventResponse upsert(CalendarSourceType sourceType, CalendarEventType eventType,
                                 CourseCalendarProjectionRequest request);

    void cancel(CalendarSourceType sourceType, UUID sourceId);
}
