package com.mentify.calendar.dto;

import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.enums.CalendarVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Client-controlled event fields. Identity, source, status, and audit fields are server-controlled. */
@Data
public class CalendarEventRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "eventType is required")
    private CalendarEventType eventType;

    @NotNull(message = "startDateTime is required")
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    @NotNull(message = "allDay is required")
    private Boolean allDay;

    private UUID courseId;

    @NotNull(message = "visibility is required")
    private CalendarVisibility visibility;

    private CalendarTargetRole targetRole;

    @Size(max = 255, message = "location must not exceed 255 characters")
    private String location;

    @AssertTrue(message = "endDateTime must not precede startDateTime")
    public boolean isDateRangeValid() {
        return startDateTime == null || endDateTime == null
                || (Boolean.TRUE.equals(allDay)
                ? !endDateTime.isBefore(startDateTime)
                : endDateTime.isAfter(startDateTime));
    }

    @AssertTrue(message = "timed events require endDateTime")
    public boolean isTimedEventComplete() {
        return allDay == null || allDay || endDateTime != null;
    }
}
