package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.ExamCalendarProjectionRequest;

import java.util.UUID;

public interface CalendarExamProjectionService {

    CalendarEventResponse upsert(ExamCalendarProjectionRequest request);

    void cancel(UUID examId);
}
