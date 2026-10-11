package com.mentify.calendar;

import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.enums.CalendarVisibility;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsAValidTimedEvent() {
        assertThat(validator.validate(validEvent())).isEmpty();
    }

    @Test
    void enforcesTitleAndLocationBoundaries() {
        CalendarEvent event = validEvent();
        event.setTitle("x".repeat(201));
        event.setLocation("x".repeat(256));

        Set<?> violations = validator.validate(event);

        assertThat(violations).hasSize(2);
        event.setTitle("x".repeat(200));
        event.setLocation("x".repeat(255));
        assertThat(validator.validate(event)).isEmpty();
    }

    @Test
    void requiresTimedEndButAllowsAnAllDayEventWithoutEnd() {
        CalendarEvent event = validEvent();
        event.setEndDateTime(null);
        assertThat(validator.validate(event)).anyMatch(v -> v.getMessage().contains("timed events"));

        event.setAllDay(true);
        assertThat(validator.validate(event)).isEmpty();
    }

    @Test
    void rejectsAnEndBeforeTheStart() {
        CalendarEvent event = validEvent();
        event.setEndDateTime(event.getStartDateTime().minusMinutes(1));
        assertThat(validator.validate(event)).anyMatch(v -> v.getMessage().contains("endDateTime"));
    }

    @Test
    void enforcesConditionalCourseRoleAndSourceReferences() {
        CalendarEvent event = validEvent();
        event.setVisibility(CalendarVisibility.COURSE);
        event.setCourseId(null);
        assertThat(validator.validate(event)).anyMatch(v -> v.getMessage().contains("courseId"));

        event.setCourseId(UUID.randomUUID());
        event.setVisibility(CalendarVisibility.ROLE);
        event.setTargetRole(null);
        assertThat(validator.validate(event)).anyMatch(v -> v.getMessage().contains("targetRole"));

        event.setTargetRole(CalendarTargetRole.TEACHER);
        event.setSourceType(CalendarSourceType.EXAM);
        event.setSourceId(null);
        assertThat(validator.validate(event)).anyMatch(v -> v.getMessage().contains("sourceId"));
    }

    private static CalendarEvent validEvent() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 12, 9, 0);
        return CalendarEvent.builder()
                .title("Calendar event")
                .eventType(CalendarEventType.CUSTOM)
                .startDateTime(start)
                .endDateTime(start.plusHours(1))
                .allDay(false)
                .sourceType(CalendarSourceType.CUSTOM)
                .createdBy(UUID.randomUUID())
                .visibility(CalendarVisibility.PRIVATE)
                .status(CalendarEventStatus.SCHEDULED)
                .createdAt(LocalDateTime.of(2026, 10, 11, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 11, 8, 0))
                .build();
    }
}
