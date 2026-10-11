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
                || !course.getAssignedTeacherId().equals(currentUser.getCurrentUserId())) {
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
