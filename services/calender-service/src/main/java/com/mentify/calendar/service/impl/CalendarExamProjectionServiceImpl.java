package com.mentify.calendar.service.impl;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.ExamCalendarProjectionRequest;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.service.CalendarExamProjectionService;
import com.mentify.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarExamProjectionServiceImpl implements CalendarExamProjectionService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;

    @Override
    @Transactional
    public synchronized CalendarEventResponse upsert(@Valid ExamCalendarProjectionRequest request) {
        if (request.getEndDateTime().isBefore(request.getStartDateTime())
                || request.getEndDateTime().equals(request.getStartDateTime())) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "Exam endDateTime must be after startDateTime");
        }

        CalendarEvent event = repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, request.getExamId())
                .orElseGet(CalendarEvent::new);
        boolean created = event.getId() == null;
        if (!created && isCurrentProjection(event, request)) {
            log.info("Exam calendar projection already current for exam {}", request.getExamId());
            return mapper.toResponse(event, false, false);
        }
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setEventType(CalendarEventType.EXAM);
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setAllDay(false);
        event.setCourseId(request.getCourseId());
        event.setSourceType(CalendarSourceType.EXAM);
        event.setSourceId(request.getExamId());
        event.setCreatedBy(request.getCreatedBy());
        event.setVisibility(CalendarVisibility.COURSE);
        event.setTargetRole(null);
        event.setStatus(CalendarEventStatus.SCHEDULED);
        event.setLocation(request.getVenue());
        CalendarEvent saved = repository.save(event);
        log.info("Exam calendar projection {} for exam {}", created ? "created" : "refreshed", request.getExamId());
        return mapper.toResponse(saved, false, false);
    }

    @Override
    @Transactional
    public void cancel(UUID examId) {
        repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, examId)
                .ifPresent(event -> {
                    if (event.getStatus() == CalendarEventStatus.CANCELLED) {
                        log.info("Exam calendar projection already cancelled for exam {}", examId);
                        return;
                    }
                    event.setStatus(CalendarEventStatus.CANCELLED);
                    repository.save(event);
                    log.info("Exam calendar projection cancelled for exam {}", examId);
                });
    }

    private boolean isCurrentProjection(CalendarEvent event, ExamCalendarProjectionRequest request) {
        return event.getStatus() == CalendarEventStatus.SCHEDULED
                && request.getCourseId().equals(event.getCourseId())
                && request.getTitle().trim().equals(event.getTitle())
                && java.util.Objects.equals(request.getDescription(), event.getDescription())
                && java.util.Objects.equals(request.getVenue(), event.getLocation())
                && request.getStartDateTime().equals(event.getStartDateTime())
                && request.getEndDateTime().equals(event.getEndDateTime())
                && request.getCreatedBy().equals(event.getCreatedBy())
                && event.getVisibility() == CalendarVisibility.COURSE
                && event.getEventType() == CalendarEventType.EXAM
                && event.getSourceType() == CalendarSourceType.EXAM;
    }
}
