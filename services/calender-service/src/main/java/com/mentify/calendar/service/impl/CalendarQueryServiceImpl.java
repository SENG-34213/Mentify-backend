package com.mentify.calendar.service.impl;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.calendar.service.CalendarCourseAccessService;
import com.mentify.calendar.service.CalendarQueryService;
import com.mentify.calendar.exception.CalendarDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarQueryServiceImpl implements CalendarQueryService {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Colombo");

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;
    private final CurrentCalendarUserService currentUser;
    private final CalendarCourseAccessService courseAccess;

    @Value("${calendar.query.max-range-days:31}")
    private long maxRangeDays;

    @Value("${calendar.query.default-upcoming-limit:20}")
    private int defaultUpcomingLimit;

    @Value("${calendar.query.max-upcoming-limit:100}")
    private int maxUpcomingLimit;

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> findEvents(LocalDateTime start, LocalDateTime end,
                                                   CalendarEventType type, UUID courseId,
                                                   CalendarEventStatus requestedStatus,
                                                   String authorizationHeader) {
        validateRange(start, end);
        CalendarEventStatus status = resolveStatus(requestedStatus);
        courseAccess.assertCanQueryCourse(courseId, authorizationHeader);

        List<CalendarEvent> events = query(start, end, type, courseId, status, authorizationHeader);
        return events.stream().map(event -> mapper.toResponse(event, false, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventResponse> findUpcoming(Integer requestedLimit, String authorizationHeader) {
        int limit = requestedLimit == null ? defaultUpcomingLimit : requestedLimit;
        if (limit < 1 || limit > maxUpcomingLimit) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + maxUpcomingLimit);
        }

        CalendarEventStatus status = resolveStatus(null);
        LocalDateTime from = LocalDateTime.now(APPLICATION_ZONE);
        List<CalendarEvent> events = upcoming(from, status, authorizationHeader);
        return events.stream().limit(limit).map(event -> mapper.toResponse(event, false, false)).toList();
    }

    private List<CalendarEvent> query(LocalDateTime start, LocalDateTime end,
                                      CalendarEventType type, UUID courseId,
                                      CalendarEventStatus status, String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return repository.findAdministrativeOverlapping(start, end, courseId, type, status);
        }
        return repository.findVisibleOverlapping(start, end, courseId, type, status,
                currentUser.getCurrentUserId(), targetRoles(), safeCourseIds(authorizationHeader));
    }

    private List<CalendarEvent> upcoming(LocalDateTime from, CalendarEventStatus status,
                                         String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return repository.findAdministrativeUpcoming(from, null, null, status);
        }
        return repository.findVisibleUpcoming(from, null, null, status,
                currentUser.getCurrentUserId(), targetRoles(), safeCourseIds(authorizationHeader));
    }

    private Set<UUID> safeCourseIds(String authorizationHeader) {
        Set<UUID> ids = courseAccess.getVisibleCourseIds(authorizationHeader);
        return ids.isEmpty() ? Set.of(new UUID(0L, 0L)) : ids;
    }

    private Set<CalendarTargetRole> targetRoles() {
        return Set.of(CalendarTargetRole.values()).stream()
                .filter(role -> currentUser.hasRole(role.name()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private CalendarEventStatus resolveStatus(CalendarEventStatus requestedStatus) {
        if (requestedStatus == CalendarEventStatus.CANCELLED
                && !currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw new AccessDeniedException("Only administrators may query cancelled events");
        }
        return requestedStatus == null ? CalendarEventStatus.SCHEDULED : requestedStatus;
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "start and end are required and end must be after start");
        }
        if (end.isAfter(start.plusDays(maxRangeDays))) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "The requested date range exceeds the maximum of " + maxRangeDays + " days");
        }
    }
}
