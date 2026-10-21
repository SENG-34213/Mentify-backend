package com.mentify.calendar.dto;

import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.enums.CalendarVisibility;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class CalendarEventResponse {
    UUID id;
    String title;
    String description;
    CalendarEventType eventType;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    boolean allDay;
    UUID courseId;
    String courseName;
    String courseCode;
    UUID instructorId;
    String instructorName;
    String instructorEmail;
    String instructorTitle;
    String instructorDepartment;
    Integer capacity;
    CalendarSourceType sourceType;
    UUID sourceId;
    UUID createdBy;
    CalendarVisibility visibility;
    CalendarTargetRole targetRole;
    CalendarEventStatus status;
    String location;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    boolean canEdit;
    boolean canCancel;
}
