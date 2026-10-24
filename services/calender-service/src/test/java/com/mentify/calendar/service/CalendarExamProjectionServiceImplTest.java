package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.ExamCalendarProjectionRequest;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.service.impl.CalendarExamProjectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarExamProjectionServiceImplTest {

    @Mock CalendarEventRepository repository;
    @Mock CalendarEventMapper mapper;
    @Mock CalendarVenueAvailabilityService venueAvailability;
    @InjectMocks CalendarExamProjectionServiceImpl service;

    private final UUID examId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID creator = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.of(2026, 10, 12, 9, 0);
    private final LocalDateTime end = start.plusHours(2);

    @Test
    void createsCourseScopedExamProjection() {
        when(repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, examId))
                .thenReturn(Optional.empty());
        CalendarEvent saved = CalendarEvent.builder().id(UUID.randomUUID()).build();
        when(repository.save(any(CalendarEvent.class))).thenReturn(saved);
        CalendarEventResponse response = CalendarEventResponse.builder().id(saved.getId()).build();
        when(mapper.toResponse(saved, false, false)).thenReturn(response);

        assertThat(service.upsert(request())).isEqualTo(response);
        verify(repository).save(any(CalendarEvent.class));
    }

    @Test
    void repeatedIdenticalProjectionDoesNotSaveAgain() {
        CalendarEvent existing = CalendarEvent.builder().id(UUID.randomUUID())
                .title("Mathematics Exam").description("Bring calculator")
                .eventType(CalendarEventType.EXAM).startDateTime(start).endDateTime(end)
                .courseId(courseId).sourceType(CalendarSourceType.EXAM).sourceId(examId)
                .createdBy(creator).visibility(CalendarVisibility.COURSE)
                .status(CalendarEventStatus.SCHEDULED).build();
        when(repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, examId))
                .thenReturn(Optional.of(existing));
        CalendarEventResponse response = CalendarEventResponse.builder().id(existing.getId()).build();
        when(mapper.toResponse(existing, false, false)).thenReturn(response);

        assertThat(service.upsert(request())).isEqualTo(response);
        verify(repository, never()).save(any(CalendarEvent.class));
    }

    @Test
    void repeatedCancellationDoesNotModifyAlreadyCancelledProjection() {
        CalendarEvent existing = CalendarEvent.builder().id(UUID.randomUUID())
                .sourceType(CalendarSourceType.EXAM).sourceId(examId)
                .status(CalendarEventStatus.CANCELLED).build();
        when(repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, examId))
                .thenReturn(Optional.of(existing));

        service.cancel(examId);

        verify(repository, never()).save(any(CalendarEvent.class));
    }

    private ExamCalendarProjectionRequest request() {
        ExamCalendarProjectionRequest request = new ExamCalendarProjectionRequest();
        request.setExamId(examId);
        request.setCourseId(courseId);
        request.setTitle("Mathematics Exam");
        request.setDescription("Bring calculator");
        request.setStartDateTime(start);
        request.setEndDateTime(end);
        request.setCreatedBy(creator);
        return request;
    }
}
