package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.mapper.AssignmentMapper;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.exception.CourseNotFoundException;
import com.mentify.quiz.exception.StudentNotEnrolledException;
import com.mentify.quiz.security.CurrentUserService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> createAssignment(CreateAssignmentRequest request, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(request.getCourseId(), authorizationHeader);
        assertCanCreateAssignmentForCourse(course);
        validateRequest(request.getStartDate(), request.getDueDate(), request.getModuleId(), request.getLessonId());

        Assignment savedAssignment = assignmentRepository.save(Assignment.builder()
                .courseId(request.getCourseId())
                .moduleId(request.getModuleId())
                .lessonId(request.getLessonId())
                .teacherId(resolveTeacherId(course))
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .instructions(trimToNull(request.getInstructions()))
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks())
                .allowedAttempts(request.getAllowedAttempts())
                .lateSubmissionAllowed(Boolean.TRUE.equals(request.getLateSubmissionAllowed()))
                .latePenaltyPercentage(normalizePenalty(request.getLatePenaltyPercentage()))
                .status(AssignmentStatus.DRAFT)
                .build());

        return response(HttpStatus.CREATED, "Assignment created successfully",
                AssignmentMapper.toAssignmentResponse(savedAssignment));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> updateAssignment(
            UUID assignmentId,
            UpdateAssignmentRequest request,
            String authorizationHeader
    ) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanManageAssignment(assignment);
        assertStatus(assignment, AssignmentStatus.DRAFT, "Assignment can only be edited while it is in DRAFT status");

        CourseLookupResponse course = getCourseOrThrow(request.getCourseId(), authorizationHeader);
        assertCanCreateAssignmentForCourse(course);
        validateRequest(request.getStartDate(), request.getDueDate(), request.getModuleId(), request.getLessonId());

        assignment.setCourseId(request.getCourseId());
        assignment.setModuleId(request.getModuleId());
        assignment.setLessonId(request.getLessonId());
        assignment.setTeacherId(resolveTeacherId(course));
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(trimToNull(request.getDescription()));
        assignment.setInstructions(trimToNull(request.getInstructions()));
        assignment.setStartDate(request.getStartDate());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());
        assignment.setAllowedAttempts(request.getAllowedAttempts());
        assignment.setLateSubmissionAllowed(Boolean.TRUE.equals(request.getLateSubmissionAllowed()));
        assignment.setLatePenaltyPercentage(normalizePenalty(request.getLatePenaltyPercentage()));

        return response(HttpStatus.OK, "Assignment updated successfully",
                AssignmentMapper.toAssignmentResponse(assignmentRepository.save(assignment)));
    }

    @Override
    public ApiResponse<AssignmentResponse> getTeacherAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanManageAssignment(assignment);
        return response(HttpStatus.OK, "Assignment fetched successfully",
                AssignmentMapper.toAssignmentResponse(assignment));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> publishAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanManageAssignment(assignment);
        assertStatus(assignment, AssignmentStatus.DRAFT, "Assignment can only be published from DRAFT status");
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        assignment.setStatus(AssignmentStatus.PUBLISHED);

        return response(HttpStatus.OK, "Assignment published successfully",
                AssignmentMapper.toAssignmentResponse(assignmentRepository.save(assignment)));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> closeAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanManageAssignment(assignment);
        assertStatus(assignment, AssignmentStatus.PUBLISHED, "Assignment can only be closed from PUBLISHED status");
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        assignment.setStatus(AssignmentStatus.CLOSED);

        return response(HttpStatus.OK, "Assignment closed successfully",
                AssignmentMapper.toAssignmentResponse(assignmentRepository.save(assignment)));
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanManageAssignment(assignment);
        assignment.setActive(false);
        assignmentRepository.save(assignment);
        return response(HttpStatus.OK, "Assignment deleted successfully", null);
    }

    @Override
    public ApiResponse<List<StudentAssignmentResponse>> getPublishedAssignmentsByCourse(
            UUID courseId,
            String authorizationHeader
    ) {
        UUID studentId = currentUserService.getCurrentUserId();
        assertStudentEnrolled(studentId, courseId, authorizationHeader);

        List<StudentAssignmentResponse> assignments = assignmentRepository
                .findByCourseIdAndStatusAndIsActiveTrueOrderByDueDateAsc(courseId, AssignmentStatus.PUBLISHED)
                .stream()
                .map(AssignmentMapper::toStudentAssignmentResponse)
                .toList();

        return response(HttpStatus.OK, "Assignments fetched successfully", assignments);
    }

    @Override
    public ApiResponse<StudentAssignmentResponse> getStudentAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Assignment", "id", assignmentId);
        }

        UUID studentId = currentUserService.getCurrentUserId();
        assertStudentEnrolled(studentId, assignment.getCourseId(), authorizationHeader);

        return response(HttpStatus.OK, "Assignment fetched successfully",
                AssignmentMapper.toStudentAssignmentResponse(assignment));
    }

    private Assignment getAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findByIdAndIsActiveTrue(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
    }

    private CourseLookupResponse getCourseOrThrow(UUID courseId, String authorizationHeader) {
        try {
            CourseLookupResponse course = courseServiceClient.getCourseById(courseId, authorizationHeader).getData();
            if (course == null || course.getId() == null) {
                throw new CourseNotFoundException(courseId);
            }
            return course;
        } catch (FeignException.NotFound ex) {
            throw new CourseNotFoundException(courseId);
        } catch (FeignException.Forbidden ex) {
            throw new AccessDeniedException("Not authorized to validate this course");
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate course", ex);
        }
    }

    private void assertStudentEnrolled(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            if (!enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, authorizationHeader)) {
                throw new StudentNotEnrolledException();
            }
        } catch (FeignException.Forbidden ex) {
            throw new StudentNotEnrolledException();
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate enrollment", ex);
        }
    }

    private void assertCanCreateAssignmentForCourse(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }

        if (currentUserService.hasAnyRole("TEACHER")
                && currentUserService.getCurrentUserId().equals(course.getAssignedTeacherId())) {
            return;
        }

        throw new AccessDeniedException("Teacher can only create assignments for assigned courses");
    }

    private UUID resolveTeacherId(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("TEACHER")) {
            return currentUserService.getCurrentUserId();
        }
        return course.getAssignedTeacherId() != null ? course.getAssignedTeacherId() : currentUserService.getCurrentUserId();
    }

    private void assertCanManageAssignment(Assignment assignment) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }

        if (currentUserService.hasAnyRole("TEACHER")
                && currentUserService.getCurrentUserId().equals(assignment.getTeacherId())) {
            return;
        }

        throw new AccessDeniedException("Teacher can only manage own assignments");
    }

    private void assertStatus(Assignment assignment, AssignmentStatus expectedStatus, String message) {
        if (assignment.getStatus() != expectedStatus) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateRequest(
            LocalDateTime startDate,
            LocalDateTime dueDate,
            UUID moduleId,
            UUID lessonId
    ) {
        if (startDate != null && !startDate.isBefore(dueDate)) {
            throw new IllegalArgumentException("Assignment start date must be before due date");
        }
        if (lessonId != null && moduleId == null) {
            throw new IllegalArgumentException("Module ID is required when lesson ID is provided");
        }
    }

    private BigDecimal normalizePenalty(BigDecimal latePenaltyPercentage) {
        if (latePenaltyPercentage == null) {
            return BigDecimal.ZERO;
        }
        return latePenaltyPercentage;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
