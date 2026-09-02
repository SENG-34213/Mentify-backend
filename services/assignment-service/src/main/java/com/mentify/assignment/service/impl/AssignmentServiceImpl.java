package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> createAssignment(CreateAssignmentRequest request, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(request.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);
        validateAssignmentDates(request.getStartDate(), request.getDueDate());

        Assignment assignment = Assignment.builder()
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
                .allowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()))
                .latePenaltyPercentage(request.getLatePenaltyPercentage() == null ? 0 : request.getLatePenaltyPercentage())
                .status(AssignmentStatus.DRAFT)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        return response(HttpStatus.CREATED, "Assignment created successfully", mapToResponse(saved));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> updateAssignment(UUID assignmentId, UpdateAssignmentRequest request, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new RuntimeException("Assignment can only be updated while it is in DRAFT status");
        }
        validateAssignmentDates(request.getStartDate(), request.getDueDate());
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);

        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(trimToNull(request.getDescription()));
        assignment.setInstructions(trimToNull(request.getInstructions()));
        assignment.setStartDate(request.getStartDate());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());
        assignment.setAllowedAttempts(request.getAllowedAttempts());
        assignment.setAllowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()));
        assignment.setLatePenaltyPercentage(request.getLatePenaltyPercentage() == null ? 0 : request.getLatePenaltyPercentage());

        return response(HttpStatus.OK, "Assignment updated successfully", mapToResponse(assignmentRepository.save(assignment)));
    }

    @Override
    public ApiResponse<AssignmentResponse> getAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        return response(HttpStatus.OK, "Assignment fetched successfully", mapToResponse(assignment));
    }

    @Override
    public ApiResponse<List<AssignmentResponse>> listAssignmentsForCourse(UUID courseId, String authorizationHeader) {
        getCourseOrThrow(courseId, authorizationHeader);
        List<AssignmentResponse> assignments = assignmentRepository.findByCourseIdAndIsActiveTrueOrderByStartDateDesc(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return response(HttpStatus.OK, "Assignments fetched successfully", assignments);
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        assignment.setActive(false);
        assignmentRepository.save(assignment);
        return response(HttpStatus.OK, "Assignment deleted successfully", null);
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> publishAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new RuntimeException("Assignment can only be published from DRAFT status");
        }

        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setPublishedAt(LocalDateTime.now());
        return response(HttpStatus.OK, "Assignment published successfully", mapToResponse(assignmentRepository.save(assignment)));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> closeAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        assignment.setStatus(AssignmentStatus.CLOSED);
        return response(HttpStatus.OK, "Assignment closed successfully", mapToResponse(assignmentRepository.save(assignment)));
    }

    private Assignment getAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findByIdAndIsActiveTrue(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
    }

    private CourseLookupResponse getCourseOrThrow(UUID courseId, String authorizationHeader) {
        if (courseServiceClient == null) {
            throw new RuntimeException("Course service is not available");
        }
        ApiResponse<CourseLookupResponse> response = courseServiceClient.getCourseById(courseId, authorizationHeader);
        if (response == null || response.getData() == null) {
            throw new RuntimeException("Course not found");
        }
        return response.getData();
    }

    private void assertTeacherCanManageCourse(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            throw new RuntimeException("Access denied");
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!currentUserId.equals(course.getAssignedTeacherId())) {
            throw new RuntimeException("Teacher can only manage assigned courses");
        }
    }

    private void assertTeacherCanManageAssignment(Assignment assignment) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            throw new RuntimeException("Access denied");
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!currentUserId.equals(assignment.getTeacherId())) {
            throw new RuntimeException("Teacher can only manage their own assignments");
        }
    }

    private UUID resolveTeacherId(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("TEACHER")) {
            return currentUserService.getCurrentUserId();
        }
        return course.getAssignedTeacherId() != null ? course.getAssignedTeacherId() : currentUserService.getCurrentUserId();
    }

    private void validateAssignmentDates(LocalDateTime startDate, LocalDateTime dueDate) {
        if (startDate == null || dueDate == null) {
            throw new RuntimeException("Start date and due date are required");
        }
        if (!startDate.isBefore(dueDate)) {
            throw new RuntimeException("Assignment start date must be before due date");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private AssignmentResponse mapToResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourseId())
                .moduleId(assignment.getModuleId())
                .lessonId(assignment.getLessonId())
                .teacherId(assignment.getTeacherId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .instructions(assignment.getInstructions())
                .startDate(assignment.getStartDate())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks())
                .allowedAttempts(assignment.getAllowedAttempts())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .latePenaltyPercentage(assignment.getLatePenaltyPercentage())
                .status(assignment.getStatus())
                .publishedAt(assignment.getPublishedAt())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
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
