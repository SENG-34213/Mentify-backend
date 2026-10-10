package com.mentify.exam.service;

import com.mentify.exam.client.CourseServiceClient;
import com.mentify.exam.client.dto.CourseLookupResponse;
import com.mentify.exam.enums.ExamRole;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Validates course existence and teacher assignment through Course Service.
 * Fails closed: any Course Service failure rejects the operation instead of bypassing authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseAccessService {

    static final String COURSE_UNAVAILABLE_MESSAGE = "Course Service is currently unavailable. Please try again later.";

    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;

    /** Ensures the caller may manage exams of the course; returns the course. */
    public CourseLookupResponse assertCanManage(UUID courseId, String authorizationHeader) {
        if (!currentUserService.hasAnyRole(ExamRole.SUPER_ADMIN, ExamRole.ADMIN, ExamRole.TEACHER)) {
            throw new AccessDeniedException("Access denied");
        }
        CourseLookupResponse course = fetchCourse(courseId, authorizationHeader);
        if (currentUserService.hasAnyRole(ExamRole.SUPER_ADMIN, ExamRole.ADMIN)) {
            return course;
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (course.getAssignedTeacherId() == null || !course.getAssignedTeacherId().equals(currentUserId)) {
            throw new AccessDeniedException("Teacher is not assigned to this course");
        }
        return course;
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
            log.warn("Course Service call failed with status {}", ex.status());
            throw new ExamDomainException(HttpStatus.SERVICE_UNAVAILABLE, COURSE_UNAVAILABLE_MESSAGE);
        }
    }
}
