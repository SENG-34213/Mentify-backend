package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.calendar.service.impl.CalendarEventServiceImpl;
import com.mentify.calendar.service.CalendarVenueAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceImplTest {

    @Mock
    private CalendarEventRepository repository;
    @Mock
    private CalendarEventMapper mapper;
    @Mock
    private CurrentCalendarUserService currentUser;
    @Mock
    private CalendarCourseAccessService courseAccess;
    @Mock
    private CalendarVenueAvailabilityService venueAvailability;
    @InjectMocks
    private CalendarEventServiceImpl service;

    private UUID userId;
    private CalendarEventRequest request;
    private CalendarEvent event;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        request = new CalendarEventRequest();
        request.setTitle("Meeting");
        request.setEventType(CalendarEventType.MEETING);
        request.setStartDateTime(LocalDateTime.of(2026, 10, 12, 9, 0));
        request.setEndDateTime(LocalDateTime.of(2026, 10, 12, 10, 0));
        request.setAllDay(false);
        request.setVisibility(CalendarVisibility.COURSE);
        request.setCourseId(UUID.randomUUID());
        event = CalendarEvent.builder()
                .id(UUID.randomUUID()).title("Meeting").eventType(CalendarEventType.MEETING)
                .startDateTime(request.getStartDateTime()).endDateTime(request.getEndDateTime())
                .allDay(false).courseId(request.getCourseId()).sourceType(CalendarSourceType.CUSTOM)
                .createdBy(userId).visibility(CalendarVisibility.COURSE).status(CalendarEventStatus.SCHEDULED)
                .build();
    }

    @Test
    void createUsesAuthenticatedIdentityAndCourseAuthorization() {
        when(currentUser.hasRole("STUDENT")).thenReturn(false);
        when(currentUser.getCurrentUserId()).thenReturn(userId);
        when(mapper.toEntity(request, userId)).thenReturn(event);
        when(repository.save(event)).thenReturn(event);
        when(mapper.toResponse(event, true, true, true))
                .thenReturn(CalendarEventResponse.builder().id(event.getId()).build());

        CalendarEventResponse response = service.create(request, "Bearer token");

        assertThat(response.getId()).isEqualTo(event.getId());
        verify(courseAccess).assertCanManage(request.getCourseId(), "Bearer token");
        verify(mapper).toEntity(request, userId);
    }

    @Test
    void studentsCannotCreateEvents() {
        when(currentUser.hasRole("STUDENT")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request, "Bearer token"))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void projectedEventsCannotBeUpdated() {
        event.setSourceType(CalendarSourceType.EXAM);
        event.setSourceId(UUID.randomUUID());
        when(repository.findById(event.getId())).thenReturn(java.util.Optional.of(event));

        assertThatThrownBy(() -> service.update(event.getId(), request, "Bearer token"))
                .isInstanceOf(CalendarDomainException.class)
                .hasMessageContaining("Source-projected");
        verify(mapper, never()).applyUpdate(any(), any());
    }

    @Test
    void cancelledEventsCannotBeUpdatedOrCancelledAgain() {
        event.setStatus(CalendarEventStatus.CANCELLED);
        when(repository.findById(event.getId())).thenReturn(java.util.Optional.of(event));
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.update(event.getId(), request, "Bearer token"))
                .isInstanceOf(CalendarDomainException.class).hasMessageContaining("cannot be updated");
        assertThatThrownBy(() -> service.cancel(event.getId(), "Bearer token"))
                .isInstanceOf(CalendarDomainException.class).hasMessageContaining("cannot be cancelled");
    }

    @Test
    void cancelRetainsTheEntityAndChangesOnlyStatus() {
        when(repository.findById(event.getId())).thenReturn(java.util.Optional.of(event));
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(repository.save(event)).thenReturn(event);
        when(mapper.toResponse(event, false, false, true))
                .thenReturn(CalendarEventResponse.builder().id(event.getId()).build());

        service.cancel(event.getId(), "Bearer token");

        assertThat(event.getStatus()).isEqualTo(CalendarEventStatus.CANCELLED);
        verify(repository).save(event);
    }

    @Test
    void teacherCannotReadHiddenPrivateEvent() {
        event.setVisibility(CalendarVisibility.PRIVATE);
        event.setCreatedBy(UUID.randomUUID());
        when(repository.findById(event.getId())).thenReturn(java.util.Optional.of(event));
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUser.getCurrentUserId()).thenReturn(userId);

        assertThatThrownBy(() -> service.get(event.getId(), "Bearer token"))
                .isInstanceOf(com.mentify.exception.ResourceNotFoundException.class);
    }
}
