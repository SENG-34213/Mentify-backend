package com.mentify.calendar.service.impl;

import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.mapper.CalendarEventMapper;
import com.mentify.calendar.repository.CalendarEventRepository;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.calendar.service.CalendarCourseAccessService;
import com.mentify.calendar.service.CalendarEventService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;
    private final CurrentCalendarUserService currentUser;
    private final CalendarCourseAccessService courseAccess;

    @Override
    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request, String authorizationHeader) {
        authorizeWriteVisibility(request, authorizationHeader);
        CalendarEvent event = repository.save(mapper.toEntity(request, currentUser.getCurrentUserId()));
        return mapper.toResponse(event, true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarEventResponse get(UUID eventId, String authorizationHeader) {
        CalendarEvent event = find(eventId);
        if (!canView(event, authorizationHeader)) {
            throw hiddenEvent(eventId);
        }
        return mapper.toResponse(event, canManage(event, authorizationHeader), canManage(event, authorizationHeader));
    }

    @Override
    @Transactional
    public CalendarEventResponse update(UUID eventId, CalendarEventRequest request, String authorizationHeader) {
        CalendarEvent event = find(eventId);
        assertManual(event, eventId);
        assertCanManage(event, eventId, authorizationHeader);
        assertNotCancelled(event, "updated");
        authorizeWriteVisibility(request, authorizationHeader);
        mapper.applyUpdate(event, request);
        return mapper.toResponse(repository.save(event), true, true);
    }

    @Override
    @Transactional
    public CalendarEventResponse cancel(UUID eventId, String authorizationHeader) {
        CalendarEvent event = find(eventId);
        assertManual(event, eventId);
        assertCanManage(event, eventId, authorizationHeader);
        assertNotCancelled(event, "cancelled");
        event.setStatus(CalendarEventStatus.CANCELLED);
        return mapper.toResponse(repository.save(event), false, false);
    }

    private void authorizeWriteVisibility(CalendarEventRequest request, String authorizationHeader) {
        if (currentUser.hasRole("STUDENT")) {
            throw new AccessDeniedException("Students cannot manage calendar events");
        }
        if (request.getVisibility() == CalendarVisibility.COURSE) {
            courseAccess.assertCanManage(request.getCourseId(), authorizationHeader);
            return;
        }
        if (request.getVisibility() == CalendarVisibility.ROLE
                || request.getVisibility() == CalendarVisibility.GLOBAL) {
            if (!currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
                throw new AccessDeniedException("Only administrators may manage role or global events");
            }
            if (request.getVisibility() == CalendarVisibility.ROLE && request.getTargetRole() == null) {
                throw new CalendarDomainException(HttpStatus.BAD_REQUEST, "targetRole is required for role events");
            }
            return;
        }
        if (request.getVisibility() == CalendarVisibility.PRIVATE
                && !currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw new AccessDeniedException("Only administrators may create private events");
        }
    }

    private boolean canView(CalendarEvent event, String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return true;
        }
        UUID userId = currentUser.getCurrentUserId();
        if (event.getVisibility() == CalendarVisibility.PRIVATE) {
            return userId.equals(event.getCreatedBy());
        }
        if (event.getVisibility() == CalendarVisibility.GLOBAL) {
            return true;
        }
        if (event.getVisibility() == CalendarVisibility.ROLE) {
            return event.getTargetRole() != null && currentUser.hasRole(event.getTargetRole().name());
        }
        if (event.getVisibility() == CalendarVisibility.COURSE) {
            if (userId.equals(event.getCreatedBy())) {
                return true;
            }
            if (currentUser.hasRole("TEACHER")) {
                try {
                    courseAccess.assertCanManage(event.getCourseId(), authorizationHeader);
                    return true;
                } catch (AccessDeniedException ex) {
                    return false;
                }
            }
            return currentUser.hasRole("STUDENT")
                    && courseAccess.canViewAsStudent(event.getCourseId(), authorizationHeader);
        }
        return false;
    }

    private boolean canManage(CalendarEvent event, String authorizationHeader) {
        if (event.getStatus() == CalendarEventStatus.CANCELLED || isProjected(event)) {
            return false;
        }
        try {
            assertCanManage(event, event.getId(), authorizationHeader);
            return true;
        } catch (AccessDeniedException | CalendarDomainException ex) {
            return false;
        }
    }

    private void assertCanManage(CalendarEvent event, UUID eventId, String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUser.hasRole("TEACHER")) {
            throw hiddenEvent(eventId);
        }
        if (currentUser.getCurrentUserId().equals(event.getCreatedBy())) {
            return;
        }
        courseAccess.assertCanManage(event.getCourseId(), authorizationHeader);
    }

    private void assertManual(CalendarEvent event, UUID eventId) {
        if (isProjected(event)) {
            throw new CalendarDomainException(HttpStatus.CONFLICT,
                    "Source-projected events cannot be managed through manual event APIs");
        }
    }

    private boolean isProjected(CalendarEvent event) {
        return event.getSourceId() != null
                || (event.getSourceType() != null
                && event.getSourceType() != com.mentify.calendar.enums.CalendarSourceType.CUSTOM
                && event.getSourceType() != com.mentify.calendar.enums.CalendarSourceType.ADMIN);
    }

    private void assertNotCancelled(CalendarEvent event, String operation) {
        if (event.getStatus() == CalendarEventStatus.CANCELLED) {
            throw new CalendarDomainException(HttpStatus.CONFLICT,
                    "Cancelled events cannot be " + operation);
        }
    }

    private CalendarEvent find(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event", "id", eventId));
    }

    private ResourceNotFoundException hiddenEvent(UUID eventId) {
        return new ResourceNotFoundException("Calendar event", "id", eventId);
    }
}
