package com.mentify.calendar.service;

import com.mentify.calendar.dto.CourseCalendarProjectionRequest;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.service.impl.CalendarCourseProjectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarCourseProjectionServiceImplTest {

    @Mock
    private CalendarEventRepository repository;
    @Mock
    private CalendarEventMapper mapper;

    private CalendarCourseProjectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalendarCourseProjectionServiceImpl(repository, mapper);
    }

    @Test
    void assignmentAndQuizWithSameUuidRemainIndependent() {
        UUID sourceId = UUID.randomUUID();
        CourseCalendarProjectionRequest request = request(sourceId);
        when(repository.findBySourceTypeAndSourceId(any(), any())).thenReturn(java.util.Optional.empty());
        when(repository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(CalendarSourceType.ASSIGNMENT, CalendarEventType.ASSIGNMENT, request);
        service.upsert(CalendarSourceType.QUIZ, CalendarEventType.QUIZ, request);

        verify(repository).findBySourceTypeAndSourceId(CalendarSourceType.ASSIGNMENT, sourceId);
        verify(repository).findBySourceTypeAndSourceId(CalendarSourceType.QUIZ, sourceId);
        verify(repository, org.mockito.Mockito.times(2)).save(any(CalendarEvent.class));
    }

    @Test
    void repeatedProjectionDoesNotSaveUnchangedEvent() {
        UUID sourceId = UUID.randomUUID();
        CourseCalendarProjectionRequest request = request(sourceId);
        CalendarEvent current = CalendarEvent.builder().id(UUID.randomUUID()).title("Task")
                .description("Description").eventType(CalendarEventType.ASSIGNMENT)
                .startDateTime(request.getStartDateTime()).endDateTime(request.getEndDateTime())
                .courseId(request.getCourseId()).sourceType(CalendarSourceType.ASSIGNMENT)
                .sourceId(sourceId).createdBy(request.getCreatedBy())
                .visibility(com.mentify.calendar.enums.CalendarVisibility.COURSE)
                .status(com.mentify.calendar.enums.CalendarEventStatus.SCHEDULED).allDay(false).build();
        when(repository.findBySourceTypeAndSourceId(CalendarSourceType.ASSIGNMENT, sourceId))
                .thenReturn(java.util.Optional.of(current));

        service.upsert(CalendarSourceType.ASSIGNMENT, CalendarEventType.ASSIGNMENT, request);

        verify(repository, never()).save(any(CalendarEvent.class));
    }

    private CourseCalendarProjectionRequest request(UUID sourceId) {
        CourseCalendarProjectionRequest request = new CourseCalendarProjectionRequest();
        request.setSourceId(sourceId);
        request.setCourseId(UUID.randomUUID());
        request.setTitle("Task");
        request.setDescription("Description");
        request.setStartDateTime(LocalDateTime.of(2026, 10, 15, 9, 0));
        request.setEndDateTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        request.setCreatedBy(UUID.randomUUID());
        return request;
    }
}
