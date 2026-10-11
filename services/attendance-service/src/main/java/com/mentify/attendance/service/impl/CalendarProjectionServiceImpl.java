package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CalendarProjectionClient;
import com.mentify.attendance.client.dto.CalendarProjectionRequest;
import com.mentify.attendance.entity.AttendanceSession;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.repository.AttendanceSessionRepository;
import com.mentify.attendance.service.CalendarProjectionAttemptService;
import com.mentify.attendance.service.CalendarProjectionService;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarProjectionServiceImpl implements CalendarProjectionService {

    private final CalendarProjectionClient calendarClient;
    private final CalendarProjectionAttemptService attempts;
    private final AttendanceSessionRepository sessionRepository;
    private final CurrentUserService currentUserService;

    @Value("${mentify.clients.calendar-service.internal-service-token:}")
    private String serviceToken;

    @Override
    public void projectIfEligible(AttendanceSession session, String authorizationHeader) {
        if (session.getStatus() == AttendanceSessionStatus.CANCELLED || !session.isActive()) {
            return;
        }
        try {
            calendarClient.upsert(toRequest(session), authorizationHeader,
                    "attendance-service", serviceToken);
            markSucceeded(session.getId());
        } catch (RuntimeException failure) {
            recordFailure(session.getId(), failure);
        }
    }

    @Override
    public void cancel(AttendanceSession session, String authorizationHeader) {
        try {
            calendarClient.cancel(session.getId(), authorizationHeader,
                    "attendance-service", serviceToken);
            markSucceeded(session.getId());
        } catch (RuntimeException failure) {
            recordFailure(session.getId(), failure);
        }
    }

    @Override
    public void reconcile(UUID sessionId, String authorizationHeader) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session", "id", sessionId));
        if (session.getStatus() == AttendanceSessionStatus.CANCELLED || !session.isActive()) {
            cancel(session, authorizationHeader);
        } else {
            projectIfEligible(session, authorizationHeader);
        }
    }

    private CalendarProjectionRequest toRequest(AttendanceSession session) {
        UUID createdBy = session.getCreatedBy() != null
                ? session.getCreatedBy()
                : currentUserService.getCurrentUserId();
        return CalendarProjectionRequest.builder()
                .sourceId(session.getId())
                .courseId(session.getCourseId())
                .title(session.getTitle())
                .startDateTime(LocalDateTime.of(session.getAttendanceDate(), session.getStartTime()))
                .endDateTime(LocalDateTime.of(session.getAttendanceDate(), session.getEndTime()))
                .createdBy(createdBy)
                .build();
    }

    private void markSucceeded(UUID sourceId) {
        try {
            attempts.succeeded(sourceId);
        } catch (RuntimeException failure) {
            log.error("Class schedule projection succeeded but outcome could not be recorded for {}", sourceId);
        }
    }

    private void recordFailure(UUID sourceId, RuntimeException failure) {
        if (failure instanceof FeignException feign) {
            log.warn("Class schedule projection failed for {} with dependency status {}", sourceId, feign.status());
        } else {
            log.warn("Class schedule projection failed for {}: {}", sourceId,
                    failure.getClass().getSimpleName());
        }
        try {
            attempts.failed(sourceId, failure);
        } catch (RuntimeException recordingFailure) {
            log.error("Class schedule projection failure could not be recorded for {}", sourceId);
        }
    }
}
