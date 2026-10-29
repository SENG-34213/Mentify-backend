package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.PatchAssignmentRequest;
import com.mentify.assignment.dto.request.RubricItemRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.RubricItemResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.AssignmentRubricItem;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.assignment.service.CalendarProjectionService;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;
    private final CalendarProjectionService calendarProjectionService;

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> createAssignment(CreateAssignmentRequest request, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(request.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);
        validateAssignmentDates(request.getStartDate(), request.getDueDate());
        validateRubricMarks(request.getRubricItems(), request.getMaxMarks());

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
                .status(Boolean.TRUE.equals(request.getPublishImmediately())
                        ? AssignmentStatus.PUBLISHED
                        : AssignmentStatus.DRAFT)
                .build();
        if (Boolean.TRUE.equals(request.getPublishImmediately())) {
            assignment.setPublishedAt(LocalDateTime.now());
        }
        replaceRubricItems(assignment, request.getRubricItems());

        Assignment saved = assignmentRepository.save(assignment);
        if (Boolean.TRUE.equals(request.getPublishImmediately())) {
            calendarProjectionService.projectAssignmentIfEligible(saved, authorizationHeader);
        }
        return response(HttpStatus.CREATED, "Assignment created successfully",
                withCourseDetails(mapToResponse(saved), course));
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
        validateRubricMarks(request.getRubricItems(), request.getMaxMarks());
        CourseLookupResponse course = getCourseOrThrow(assignment.getCourseId(), authorizationHeader);

        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(trimToNull(request.getDescription()));
        assignment.setInstructions(trimToNull(request.getInstructions()));
        assignment.setStartDate(request.getStartDate());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());
        assignment.setAllowedAttempts(request.getAllowedAttempts());
        assignment.setAllowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()));
        assignment.setLatePenaltyPercentage(request.getLatePenaltyPercentage() == null ? 0 : request.getLatePenaltyPercentage());
        if (request.getRubricItems() != null) {
            replaceRubricItems(assignment, request.getRubricItems());
        }

        Assignment saved = assignmentRepository.save(assignment);
        calendarProjectionService.projectAssignmentIfEligible(saved, authorizationHeader);
        return response(HttpStatus.OK, "Assignment updated successfully",
                withCourseDetails(mapToResponse(saved), course));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> patchAssignment(
            UUID assignmentId,
            PatchAssignmentRequest request,
            String authorizationHeader
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("At least one assignment field must be provided");
        }
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new RuntimeException("Assignment can only be updated while in DRAFT status");
        }
        CourseLookupResponse course = getCourseOrThrow(assignment.getCourseId(), authorizationHeader);

        LocalDateTime startDate = request.getStartDate() == null ? assignment.getStartDate() : request.getStartDate();
        LocalDateTime dueDate = request.getDueDate() == null ? assignment.getDueDate() : request.getDueDate();
        validateAssignmentDates(startDate, dueDate);
        int maxMarks = request.getMaxMarks() == null ? assignment.getMaxMarks() : request.getMaxMarks();
        validateRubricMarks(request.getRubricItems() == null
                ? assignment.getRubricItems().stream()
                        .map(item -> RubricItemRequest.builder()
                                .criterion(item.getCriterion())
                                .marks(item.getMarks())
                                .build())
                        .toList()
                : request.getRubricItems(), maxMarks);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new IllegalArgumentException("Assignment title cannot be blank");
            }
            assignment.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            assignment.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getInstructions() != null) {
            assignment.setInstructions(trimToNull(request.getInstructions()));
        }
        assignment.setStartDate(startDate);
        assignment.setDueDate(dueDate);
        assignment.setMaxMarks(maxMarks);
        if (request.getAllowedAttempts() != null) {
            assignment.setAllowedAttempts(request.getAllowedAttempts());
        }
        if (request.getAllowLateSubmission() != null) {
            assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        }
        if (request.getLatePenaltyPercentage() != null) {
            assignment.setLatePenaltyPercentage(request.getLatePenaltyPercentage());
        }
        if (request.getRubricItems() != null) {
            replaceRubricItems(assignment, request.getRubricItems());
        }

        Assignment saved = assignmentRepository.save(assignment);
        calendarProjectionService.projectAssignmentIfEligible(saved, authorizationHeader);
        return response(HttpStatus.OK, "Assignment updated successfully",
                withCourseDetails(mapToResponse(saved), course));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> getAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        return response(HttpStatus.OK, "Assignment fetched successfully", mapToResponse(
                assignment,
                submissionRepository.findSubmissionCountsByAssignmentIds(
                        List.of(assignmentId)
                ).stream().findFirst().orElse(null)
        ));
    }

    @Override
    @Transactional
    public ApiResponse<List<AssignmentResponse>> listAssignmentsForCourse(UUID courseId, String authorizationHeader) {
        getCourseOrThrow(courseId, authorizationHeader);
        List<Assignment> assignmentEntities =
                assignmentRepository.findByCourseIdAndIsActiveTrueOrderByStartDateDesc(courseId);
        if (assignmentEntities.isEmpty()) {
            return response(HttpStatus.OK, "Assignments fetched successfully", List.of());
        }
        List<UUID> assignmentIds = assignmentEntities.stream().map(Assignment::getId).toList();
        Map<UUID, SubmissionRepository.AssignmentSubmissionCounts> countsByAssignmentId =
                submissionRepository.findSubmissionCountsByAssignmentIds(assignmentIds).stream()
                        .collect(Collectors.toMap(
                                SubmissionRepository.AssignmentSubmissionCounts::getAssignmentId,
                                Function.identity()
                        ));
        List<AssignmentResponse> assignments = assignmentEntities.stream()
                .map(assignment -> mapToResponse(assignment, countsByAssignmentId.get(assignment.getId())))
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
        Assignment saved = assignmentRepository.save(assignment);
        calendarProjectionService.cancelAssignment(saved, authorizationHeader);
        return response(HttpStatus.OK, "Assignment deleted successfully", null);
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> publishAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        CourseLookupResponse course = getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new RuntimeException("Assignment can only be published from DRAFT status");
        }

        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setPublishedAt(LocalDateTime.now());
        Assignment saved = assignmentRepository.save(assignment);
        calendarProjectionService.projectAssignmentIfEligible(saved, authorizationHeader);
        return response(HttpStatus.OK, "Assignment published successfully",
                withCourseDetails(mapToResponse(saved), course));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentResponse> closeAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        CourseLookupResponse course = getCourseOrThrow(assignment.getCourseId(), authorizationHeader);
        assignment.setStatus(AssignmentStatus.CLOSED);
        Assignment saved = assignmentRepository.save(assignment);
        calendarProjectionService.cancelAssignment(saved, authorizationHeader);
        return response(HttpStatus.OK, "Assignment closed successfully",
                withCourseDetails(mapToResponse(saved), course));
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

    private void replaceRubricItems(Assignment assignment, List<RubricItemRequest> rubricItems) {
        assignment.getRubricItems().clear();
        if (rubricItems == null) {
            return;
        }

        for (int index = 0; index < rubricItems.size(); index++) {
            RubricItemRequest rubricItem = rubricItems.get(index);
            assignment.getRubricItems().add(AssignmentRubricItem.builder()
                    .assignment(assignment)
                    .criterion(rubricItem.getCriterion().trim())
                    .marks(rubricItem.getMarks())
                    .displayOrder(index)
                    .build());
        }
    }

    private void validateRubricMarks(List<RubricItemRequest> rubricItems, int maxMarks) {
        if (rubricItems == null) {
            return;
        }
        int totalRubricMarks = rubricItems.stream()
                .map(RubricItemRequest::getMarks)
                .filter(marks -> marks != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (totalRubricMarks > maxMarks) {
            throw new IllegalArgumentException("Total rubric marks cannot exceed assignment maximum marks");
        }
    }

    private AssignmentResponse mapToResponse(Assignment assignment) {
        SubmissionRepository.AssignmentSubmissionCounts counts =
                submissionRepository.findSubmissionCountsByAssignmentIds(
                        List.of(assignment.getId())
                ).stream().findFirst().orElse(null);
        return mapToResponse(assignment, counts);
    }

    private AssignmentResponse withCourseDetails(AssignmentResponse response, CourseLookupResponse course) {
        return response.toBuilder()
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .build();
    }

    private AssignmentResponse mapToResponse(
            Assignment assignment,
            SubmissionRepository.AssignmentSubmissionCounts counts
    ) {
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
                .rubricItems(assignment.getRubricItems() == null
                        ? List.of()
                        : assignment.getRubricItems().stream()
                                .map(item -> RubricItemResponse.builder()
                                        .id(item.getId())
                                        .criterion(item.getCriterion())
                                        .marks(item.getMarks())
                                        .displayOrder(item.getDisplayOrder())
                                        .build())
                                .toList())
                .totalSubmissions(counts == null ? 0L : counts.getTotalSubmissions())
                .gradedCount(counts == null ? 0L : counts.getGradedSubmissions())
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
