package com.mentify.service.impl;

import com.mentify.client.EntrollmentServiceClient;
import com.mentify.dto.InternalLessonResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Lesson;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LessonRepository;
import com.mentify.service.InternalLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalLessonServiceImpl implements InternalLessonService {

    private final LessonRepository lessonRepository;
    private final EntrollmentServiceClient entrollmentServiceClient;

    @Override
    public ApiResponse<InternalLessonResponse> getLessonForAi(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        validateAccess(lesson.getModule().getCourse());

        InternalLessonResponse response = InternalLessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .courseId(lesson.getModule().getCourse().getId())
                .build();

        return ApiResponse.<InternalLessonResponse>builder()
                .message("Lesson fetched successfully for AI")
                .data(response)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private void validateAccess(Course course) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
            return;
        }

        UUID userId = getCurrentUserId(authentication);

        if (roles.contains("ROLE_TEACHER")) {
            if (!course.getAssignedTeacherId().equals(userId)) {
                throw new AccessDeniedException("Teacher does not have access to this course");
            }
        } else if (roles.contains("ROLE_STUDENT")) {
            if (!entrollmentServiceClient.isStudentEnrolledInCourse(userId, course.getId())) {
                throw new AccessDeniedException("Student is not enrolled in this course");
            }
        } else {
            throw new AccessDeniedException("User role not authorized to access lesson");
        }
    }

    private UUID getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
             // Try local_user_id claim as seen in TeacherCourseAccessGuard
            subject = jwt.getClaimAsString("local_user_id");
        }
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("User identity is missing in token");
        }
        return UUID.fromString(subject);
    }
}
