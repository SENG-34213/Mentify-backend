package com.mentify.quiz.service;

import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.exception.CourseNotFoundException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.security.CurrentUserService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseQuizAuthorizationService {

    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;

    public CourseLookupResponse assertCanCreateQuizForCourse(UUID courseId, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return course;
        }

//        if (currentUserService.hasAnyRole("TEACHER")
//                && currentUserService.getCurrentUserId().equals(course.getAssignedTeacherId())) {
//            return course;
//        }

//        throw new UnauthorizedQuizAccessException("Teacher can only create quizzes for assigned courses");
        return course;
    }

    public CourseLookupResponse getCourseOrThrow(UUID courseId, String authorizationHeader) {
        try {
            CourseLookupResponse course = courseServiceClient.getCourseById(courseId, authorizationHeader).getData();
            if (course == null || course.getId() == null) {
                throw new CourseNotFoundException(courseId);
            }
            return course;
        } catch (FeignException.NotFound ex) {
            throw new CourseNotFoundException(courseId);
        } catch (FeignException.Forbidden ex) {
            throw new UnauthorizedQuizAccessException("Not authorized to validate this course");
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate course", ex);
        }
    }

    public UUID resolveTeacherId(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("TEACHER")) {
            return currentUserService.getCurrentUserId();
        }
        return course.getAssignedTeacherId() != null ? course.getAssignedTeacherId() : currentUserService.getCurrentUserId();
    }
}
