package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CalendarProjectionClient;
import com.mentify.attendance.client.dto.CalendarProjectionRequest;
import com.mentify.attendance.entity.AttendanceSession;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.repository.AttendanceSessionRepository;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.attendance.service.CalendarProjectionAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarProjectionServiceImplTest {

    @Mock
    private CalendarProjectionClient calendarClient;
    @Mock
    private CalendarProjectionAttemptService attempts;
    @Mock
    private AttendanceSessionRepository sessionRepository;
    @Mock
    private CurrentUserService currentUserService;

    private CalendarProjectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalendarProjectionServiceImpl(calendarClient, attempts, sessionRepository, currentUserService);
    }

    @Test
    void openSessionIsProjectedWithCourseClassMapping() {
        UUID sessionId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        AttendanceSession session = AttendanceSession.builder()
                .courseId(UUID.randomUUID()).title("Java Class")
                .attendanceDate(LocalDate.of(2026, 10, 16))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .status(AttendanceSessionStatus.OPEN).build();
        session.setId(sessionId);
        session.setCreatedBy(creatorId);
        session.setActive(true);

        service.projectIfEligible(session, "Bearer token");

        ArgumentCaptor<CalendarProjectionRequest> captor = ArgumentCaptor.forClass(CalendarProjectionRequest.class);
        verify(calendarClient).upsert(captor.capture(), any(), any(), any());
        assertThat(captor.getValue().getSourceId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getCourseId()).isEqualTo(session.getCourseId());
        assertThat(captor.getValue().getStartDateTime()).isEqualTo(session.getAttendanceDate().atTime(session.getStartTime()));
        assertThat(captor.getValue().getEndDateTime()).isEqualTo(session.getAttendanceDate().atTime(session.getEndTime()));
        verify(attempts).succeeded(sessionId);
    }

    @Test
    void cancelledSessionIsNotUpserted() {
        AttendanceSession session = AttendanceSession.builder().status(AttendanceSessionStatus.CANCELLED).build();
        session.setId(UUID.randomUUID());
        session.setActive(true);

        service.projectIfEligible(session, "Bearer token");

        verify(calendarClient, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void calendarFailureIsRecordedWithoutEscaping() {
        AttendanceSession session = AttendanceSession.builder()
                .courseId(UUID.randomUUID()).title("Java Class")
                .attendanceDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .status(AttendanceSessionStatus.OPEN).build();
        session.setId(UUID.randomUUID());
        session.setCreatedBy(UUID.randomUUID());
        session.setActive(true);
        when(calendarClient.upsert(any(), any(), any(), any())).thenThrow(new RuntimeException("dependency"));

        service.projectIfEligible(session, "Bearer token");

        verify(attempts).failed(eq(session.getId()), any(RuntimeException.class));
    }
}
