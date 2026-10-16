package com.mentify.calendar.mapper;

import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarSourceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CalendarEventMapper {

    public CalendarEvent toEntity(CalendarEventRequest request, UUID creator) {
        return CalendarEvent.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .allDay(request.getAllDay())
                .courseId(request.getCourseId())
                .sourceType(CalendarSourceType.CUSTOM)
                .createdBy(creator)
                .visibility(request.getVisibility())
                .targetRole(request.getTargetRole())
                .status(CalendarEventStatus.SCHEDULED)
                .location(request.getLocation())
                .build();
    }

    public void applyUpdate(CalendarEvent event, CalendarEventRequest request) {
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setAllDay(request.getAllDay());
        event.setCourseId(request.getCourseId());
        event.setVisibility(request.getVisibility());
        event.setTargetRole(request.getTargetRole());
        event.setLocation(request.getLocation());
    }

    public CalendarEventResponse toResponse(CalendarEvent event, boolean canEdit, boolean canCancel) {
        return CalendarEventResponse.builder()
                .id(event.getId()).title(event.getTitle()).description(event.getDescription())
                .eventType(event.getEventType()).startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime()).allDay(event.isAllDay()).courseId(event.getCourseId())
                .sourceType(event.getSourceType()).sourceId(event.getSourceId()).createdBy(event.getCreatedBy())
                .visibility(event.getVisibility()).targetRole(event.getTargetRole()).status(event.getStatus())
                .location(event.getLocation()).createdAt(event.getCreatedAt()).updatedAt(event.getUpdatedAt())
                .canEdit(canEdit).canCancel(canCancel).build();
    }
}
