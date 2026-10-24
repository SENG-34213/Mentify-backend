package com.mentify.calendar.service;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.calendar.service.impl.CalendarQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceImplTest {

    @Mock CalendarEventRepository repository;
    @Mock CalendarEventMapper mapper;
    @Mock CurrentCalendarUserService currentUser;
    @Mock CalendarCourseAccessService courseAccess;
    @InjectMocks CalendarQueryServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.of(2026, 10, 11, 9, 0);
    private final LocalDateTime end = start.plusDays(1);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxRangeDays", 31L);
        ReflectionTestUtils.setField(service, "defaultUpcomingLimit", 20);
        ReflectionTestUtils.setField(service, "maxUpcomingLimit", 100);
    }

    @Test
    void rejectsInvalidAndOverlongRanges() {
        assertThatThrownBy(() -> service.findEvents(end, start, null, null, null, "Bearer token"))
                .isInstanceOf(com.mentify.calendar.exception.CalendarDomainException.class);
        assertThatThrownBy(() -> service.findEvents(start, start.plusDays(32), null, null, null, "Bearer token"))
                .isInstanceOf(com.mentify.calendar.exception.CalendarDomainException.class);
    }

    @Test
    void rejectsCancelledQueriesForNonAdministrators() {
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        assertThatThrownBy(() -> service.findEvents(start, end, null, null,
                CalendarEventStatus.CANCELLED, "Bearer token"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void studentQueryUsesEnrollmentScopeAndFiltersScheduledEvents() {
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUser.hasRole(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "STUDENT".equals(invocation.getArgument(0)));
        when(currentUser.getCurrentUserId()).thenReturn(userId);
        UUID courseId = UUID.randomUUID();
        when(courseAccess.getVisibleCourseIds("Bearer token")).thenReturn(Set.of(courseId));
        CalendarEvent event = CalendarEvent.builder().id(UUID.randomUUID()).build();
        CalendarEventResponse response = CalendarEventResponse.builder().id(event.getId()).build();
        when(repository.findVisibleOverlapping(eq(start), eq(end), eq(null), eq(null),
                eq(null), eq(userId), any(), eq(Set.of(courseId))))
                .thenReturn(List.of(event));
        when(mapper.toResponse(event, false, false, false)).thenReturn(response);

        assertThat(service.findEvents(start, end, null, null, null, "Bearer token"))
                .containsExactly(response);
        verify(courseAccess).getVisibleCourseIds("Bearer token");
    }

    @Test
    void upcomingUsesApplicationClockAndAppliesLimit() {
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        CalendarEvent first = CalendarEvent.builder().id(UUID.randomUUID()).build();
        CalendarEvent second = CalendarEvent.builder().id(UUID.randomUUID()).build();
        when(repository.findAdministrativeUpcoming(any(), eq(null), eq(null), eq(CalendarEventStatus.SCHEDULED)))
                .thenReturn(List.of(first, second));
        when(mapper.toResponse(any(CalendarEvent.class), eq(true), eq(true), eq(true)))
                .thenReturn(CalendarEventResponse.builder().build());

        assertThat(service.findUpcoming(1, "Bearer token")).hasSize(1);
    }
}
