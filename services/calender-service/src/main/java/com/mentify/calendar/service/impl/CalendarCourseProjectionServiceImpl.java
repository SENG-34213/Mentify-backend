package com.mentify.calendar.service.impl;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.dto.CourseCalendarProjectionRequest;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.service.CalendarCourseProjectionService;
import com.mentify.calendar.service.CalendarVenueAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarCourseProjectionServiceImpl implements CalendarCourseProjectionService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;
    private final CalendarVenueAvailabilityService venueAvailability;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized CalendarEventResponse upsert(CalendarSourceType sourceType,
                                                     CalendarEventType eventType,
                                                     CourseCalendarProjectionRequest request) {
        if (sourceType != CalendarSourceType.ASSIGNMENT && sourceType != CalendarSourceType.QUIZ
                && sourceType != CalendarSourceType.COURSE_CLASS
                && sourceType != CalendarSourceType.ATTENDANCE) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST, "Unsupported course projection source type");
        }
        if (eventType != CalendarEventType.ASSIGNMENT && eventType != CalendarEventType.QUIZ
                && eventType != CalendarEventType.CLASS
                && eventType != CalendarEventType.ATTENDANCE) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST, "Unsupported course projection event type");
        }
        if ((sourceType == CalendarSourceType.ASSIGNMENT && eventType != CalendarEventType.ASSIGNMENT)
                || (sourceType == CalendarSourceType.QUIZ && eventType != CalendarEventType.QUIZ)
                || (sourceType == CalendarSourceType.COURSE_CLASS && eventType != CalendarEventType.CLASS)
                || (sourceType == CalendarSourceType.ATTENDANCE && eventType != CalendarEventType.ATTENDANCE)) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST, "Projection source and event types must match");
        }
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "Projection endDateTime must be after startDateTime");
        }

        CalendarEvent event = repository.findBySourceTypeAndSourceId(sourceType, request.getSourceId())
                .orElseGet(CalendarEvent::new);
        boolean created = event.getId() == null;
        if (!created && isCurrentProjection(event, sourceType, eventType, request)) {
            log.info("{} calendar projection already current for source {}", sourceType, request.getSourceId());
            return mapper.toResponse(event, false, false);
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setEventType(eventType);
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setAllDay(false);
        event.setCourseId(request.getCourseId());
        event.setSourceType(sourceType);
        event.setSourceId(request.getSourceId());
        event.setCreatedBy(request.getCreatedBy());
        event.setVisibility(CalendarVisibility.COURSE);
        event.setTargetRole(null);
        event.setStatus(CalendarEventStatus.SCHEDULED);
        event.setLocation(null);
        venueAvailability.assertAvailable(event, event.getId());
        CalendarEvent saved = repository.save(event);
        log.info("{} calendar projection {} for source {}", sourceType,
                created ? "created" : "refreshed", request.getSourceId());
        return mapper.toResponse(saved, false, false);
    }

    @Override
    @Transactional
    public void cancel(CalendarSourceType sourceType, UUID sourceId) {
        repository.findBySourceTypeAndSourceId(sourceType, sourceId).ifPresent(event -> {
            if (event.getStatus() == CalendarEventStatus.CANCELLED) {
                log.info("{} calendar projection already cancelled for source {}", sourceType, sourceId);
                return;
            }
            event.setStatus(CalendarEventStatus.CANCELLED);
            repository.save(event);
            log.info("{} calendar projection cancelled for source {}", sourceType, sourceId);
        });
    }

    private boolean isCurrentProjection(CalendarEvent event, CalendarSourceType sourceType,
                                        CalendarEventType eventType, CourseCalendarProjectionRequest request) {
        return event.getStatus() == CalendarEventStatus.SCHEDULED
                && Objects.equals(request.getCourseId(), event.getCourseId())
                && request.getTitle().trim().equals(event.getTitle())
                && Objects.equals(request.getDescription(), event.getDescription())
                && request.getStartDateTime().equals(event.getStartDateTime())
                && request.getEndDateTime().equals(event.getEndDateTime())
                && event.getVisibility() == CalendarVisibility.COURSE
                && event.getEventType() == eventType
                && event.getSourceType() == sourceType;
    }
}
