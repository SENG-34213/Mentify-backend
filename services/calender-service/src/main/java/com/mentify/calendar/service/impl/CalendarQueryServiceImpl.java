package com.mentify.calendar.service.impl;

import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.config.CalendarTimeZone;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.calendar.service.CalendarCourseAccessService;
import com.mentify.calendar.service.CalendarQueryService;
import com.mentify.calendar.client.CourseServiceClient;
import com.mentify.calendar.client.UserServiceClient;
import com.mentify.calendar.client.dto.CourseLookupResponse;
import com.mentify.calendar.client.dto.TeacherLookupResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.calendar.exception.CalendarDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarQueryServiceImpl implements CalendarQueryService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;
    private final CurrentCalendarUserService currentUser;
    private final CalendarCourseAccessService courseAccess;
    private final CourseServiceClient courseServiceClient;
    private final UserServiceClient userServiceClient;

    @Value("${calendar.query.max-range-days:31}")
    private long maxRangeDays;

    @Value("${calendar.query.default-upcoming-limit:20}")
    private int defaultUpcomingLimit;

    @Value("${calendar.query.max-upcoming-limit:100}")
    private int maxUpcomingLimit;

    @Override
    public List<CalendarEventResponse> findEvents(LocalDateTime start, LocalDateTime end,
                                                   CalendarEventType type, UUID courseId,
                                                   CalendarEventStatus requestedStatus,
                                                   String authorizationHeader) {
        validateRange(start, end);
        CalendarEventStatus status = resolveStatus(requestedStatus);
        Set<UUID> visibleCourseIds = visibleCourseIds(authorizationHeader);
        if (courseId != null) {
            if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
                courseAccess.assertCanQueryCourse(courseId, authorizationHeader);
            } else if (!visibleCourseIds.contains(courseId)) {
                throw new AccessDeniedException("You are not authorized to query this course");
            }
        }
        List<CalendarEvent> events = query(start, end, type, courseId, status, visibleCourseIds);
        return events.stream().map(event -> enrich(event, authorizationHeader, visibleCourseIds)).toList();
    }

    @Override
    public List<CalendarEventResponse> findUpcoming(Integer requestedLimit, String authorizationHeader) {
        int limit = requestedLimit == null ? defaultUpcomingLimit : requestedLimit;
        if (limit < 1 || limit > maxUpcomingLimit) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + maxUpcomingLimit);
        }

        CalendarEventStatus status = CalendarEventStatus.SCHEDULED;
        LocalDateTime from = LocalDateTime.now(CalendarTimeZone.current());
        Set<UUID> visibleCourseIds = visibleCourseIds(authorizationHeader);
        List<CalendarEvent> events = upcoming(from, status, visibleCourseIds);
        return events.stream().limit(limit)
                .map(event -> enrich(event, authorizationHeader, visibleCourseIds)).toList();
    }

    private CalendarEventResponse enrich(CalendarEvent event, String authorizationHeader,
                                         Set<UUID> visibleCourseIds) {
        boolean canManage = canManage(event, authorizationHeader);
        boolean active = event.getStatus() != CalendarEventStatus.CANCELLED;
        CalendarEventResponse response = mapper.toResponse(event, canManage && active,
                canManage && active, canManage);
        if (event.getCourseId() == null) return response;
        try {
            ApiResponse<CourseLookupResponse> lookup = courseServiceClient.getCourseById(event.getCourseId(), authorizationHeader);
            CourseLookupResponse course = lookup == null ? null : lookup.getData();
            if (course == null) return response;
            CalendarEventResponse enriched = response.toBuilder()
                    .courseName(course.getCourseName())
                    .courseCode(course.getCourseCode())
                    .instructorId(course.getAssignedTeacherId())
                    .capacity(course.getMaxCapacity())
                    .build();
            if (course.getAssignedTeacherId() == null) return enriched;
            try {
                ApiResponse<TeacherLookupResponse> teacherLookup = userServiceClient.getTeacherById(
                        course.getAssignedTeacherId(), authorizationHeader);
                TeacherLookupResponse teacher = teacherLookup == null ? null : teacherLookup.getData();
                if (teacher == null) return enriched;
                return enriched.toBuilder()
                        .instructorName(teacher.getFullName())
                        .instructorEmail(teacher.getEmail())
                        .instructorTitle(teacher.getTitle())
                        .instructorDepartment(teacher.getDepartment())
                        .build();
            } catch (RuntimeException ignored) {
                return enriched;
            }
        } catch (RuntimeException exception) {
            return response;
        }
    }

    private boolean canManage(CalendarEvent event, String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) return true;
        if (event.getSourceId() != null
                || (event.getSourceType() != null
                && event.getSourceType() != com.mentify.calendar.enums.CalendarSourceType.CUSTOM
                && event.getSourceType() != com.mentify.calendar.enums.CalendarSourceType.ADMIN)) {
            return false;
        }
        if (!currentUser.hasRole("TEACHER")) return false;
        if (currentUser.getIdentityIds().contains(event.getCreatedBy())) return true;
        if (event.getSourceType() == com.mentify.calendar.enums.CalendarSourceType.ADMIN
                && event.getCourseId() == null) return false;
        try {
            courseAccess.assertCanManage(event.getCourseId(), authorizationHeader);
            return true;
        } catch (AccessDeniedException | CalendarDomainException exception) {
            return false;
        }
    }

    private List<CalendarEvent> query(LocalDateTime start, LocalDateTime end,
                                      CalendarEventType type, UUID courseId,
                                      CalendarEventStatus status, Set<UUID> visibleCourseIds) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return repository.findAdministrativeOverlapping(start, end, courseId, type, status);
        }
        if (visibleCourseIds.isEmpty()) {
            return repository.findVisibleOverlappingWithoutCourses(start, end, courseId, type, status,
                    currentUser.getCurrentUserId(), targetRoles());
        }
        return repository.findVisibleOverlapping(start, end, courseId, type, status,
                currentUser.getCurrentUserId(), targetRoles(), visibleCourseIds);
    }

    private List<CalendarEvent> upcoming(LocalDateTime from, CalendarEventStatus status,
                                         Set<UUID> visibleCourseIds) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return repository.findAdministrativeUpcoming(from, null, null, status);
        }
        if (visibleCourseIds.isEmpty()) {
            return repository.findVisibleUpcomingWithoutCourses(from, null, null, status,
                    currentUser.getCurrentUserId(), targetRoles());
        }
        return repository.findVisibleUpcoming(from, null, null, status,
                currentUser.getCurrentUserId(), targetRoles(), visibleCourseIds);
    }

    private Set<UUID> visibleCourseIds(String authorizationHeader) {
        return currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")
                ? Set.of() : courseAccess.getVisibleCourseIds(authorizationHeader);
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
        return requestedStatus;
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
