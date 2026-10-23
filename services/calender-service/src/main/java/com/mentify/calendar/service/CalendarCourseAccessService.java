package com.mentify.calendar.service;

import com.mentify.calendar.client.CourseServiceClient;
import com.mentify.calendar.client.EnrollmentServiceClient;
import com.mentify.calendar.client.dto.CourseLookupResponse;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarCourseAccessService {

    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentCalendarUserService currentUser;

    public CalendarCourseAccessService(CourseServiceClient courseServiceClient,
                                       EnrollmentServiceClient enrollmentServiceClient,
                                       CurrentCalendarUserService currentUser) {
        this.courseServiceClient = courseServiceClient;
        this.enrollmentServiceClient = enrollmentServiceClient;
        this.currentUser = currentUser;
    }

    public void assertCanManage(UUID courseId, String authorizationHeader) {
        if (courseId == null) {
            throw new CalendarDomainException(HttpStatus.BAD_REQUEST, "courseId is required for course events");
        }
        CourseLookupResponse course = fetchCourse(courseId, authorizationHeader);
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUser.hasRole("TEACHER")
                || course.getAssignedTeacherId() == null
                || !currentUser.getIdentityIds().contains(course.getAssignedTeacherId())) {
            throw new AccessDeniedException("Teacher is not assigned to this course");
        }
    }

    public boolean canViewAsStudent(UUID courseId, String authorizationHeader) {
        if (courseId == null) {
            return false;
        }
        fetchCourse(courseId, authorizationHeader);
        try {
            return enrollmentServiceClient.isStudentEnrolledInCourse(
                    currentUser.getCurrentUserId(), courseId, authorizationHeader);
        } catch (FeignException ex) {
            throw new CalendarDomainException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A dependent service is currently unavailable. Please try again later.");
        }
    }

    public Set<UUID> getVisibleCourseIds(String authorizationHeader) {
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return Collections.emptySet();
        }
        try {
            if (currentUser.hasRole("TEACHER")) {
                var response = courseServiceClient.getMyCourses(authorizationHeader);
                return response == null || response.getData() == null
                        ? Set.of()
                        : response.getData().stream().map(CourseLookupResponse::getId)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            }
            if (currentUser.hasRole("STUDENT")) {
                var response = enrollmentServiceClient.getActiveCourseIdsForStudent(
                        currentUser.getCurrentUserId(), authorizationHeader);
                return response == null || response.getData() == null
                        ? Set.of() : Set.copyOf(response.getData());
            }
            throw new AccessDeniedException("Calendar query is not available for this role");
        } catch (FeignException ex) {
            throw new CalendarDomainException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A dependent service is currently unavailable. Please try again later.");
        }
    }

    public void assertCanQueryCourse(UUID courseId, String authorizationHeader) {
        if (courseId == null) {
            return;
        }
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            fetchCourse(courseId, authorizationHeader);
            return;
        }
        if (!getVisibleCourseIds(authorizationHeader).contains(courseId)) {
            throw new AccessDeniedException("You are not authorized to query this course");
        }
    }

    private CourseLookupResponse fetchCourse(UUID courseId, String authorizationHeader) {
        try {
            var response = courseServiceClient.getCourseById(courseId, authorizationHeader);
            CourseLookupResponse course = response == null ? null : response.getData();
            if (course == null || course.getId() == null) {
                throw new ResourceNotFoundException("Course", "id", courseId);
            }
            return course;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        } catch (FeignException ex) {
            throw new CalendarDomainException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A dependent service is currently unavailable. Please try again later.");
        }
    }
}
