package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.StudentAssignmentService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.client.EnrollmentServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAssignmentServiceImpl implements StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public Page<StudentAssignmentResponse> getAssignments(
            UUID studentId, UUID courseId, String status,
            LocalDateTime dueFrom, LocalDateTime dueTo,
            int page, int size, String authorizationHeader) {
        UUID authenticatedStudentId = currentUserService.getCurrentUserId();
        if (!authenticatedStudentId.equals(studentId)) {
            throw new AccessDeniedException("Students can only view their own assignments");
        }

        List<UUID> courseIds;
        try {
            ApiResponse<List<UUID>> enrollmentResponse = enrollmentServiceClient
                    .getStudentCourseIds(authenticatedStudentId, authorizationHeader);
            if (enrollmentResponse == null || enrollmentResponse.getData() == null) {
                throw new IllegalStateException("Enrollment service returned no course list");
            }
            courseIds = enrollmentResponse.getData().stream().distinct().toList();
        } catch (FeignException ex) {
            throw new IllegalStateException("Enrollment service is unavailable", ex);
        }
        if (courseIds.isEmpty() || (courseId != null && !courseIds.contains(courseId))) {
            return Page.empty(PageRequest.of(page, size));
        }
        if (courseId != null) {
            courseIds = List.of(courseId);
        }

        List<Assignment> courseAssignments = assignmentRepository
                .findByCourseIdInAndIsActiveTrueOrderByDueDateAsc(courseIds);
        Map<UUID, Submission> latestSubmissions = courseAssignments.isEmpty() ? Map.of()
                : submissionRepository.findByAssignmentIdInAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(
                                courseAssignments.stream().map(Assignment::getId).toList(), authenticatedStudentId)
                        .stream().collect(Collectors.toMap(
                                submission -> submission.getAssignment().getId(),
                                Function.identity(), (first, second) -> first));

        List<Assignment> assignments = courseAssignments.stream()
                .filter(assignment -> assignment.getStatus() != AssignmentStatus.DRAFT)
                .filter(assignment -> status == null || matchesStatus(assignment, status,
                        latestSubmissions.get(assignment.getId())))
                .filter(assignment -> dueFrom == null || !assignment.getDueDate().isBefore(dueFrom))
                .filter(assignment -> dueTo == null || !assignment.getDueDate().isAfter(dueTo))
                .toList();

        long requestedOffset = (long) page * size;
        int start = (int) Math.min(requestedOffset, assignments.size());
        int end = Math.min(start + size, assignments.size());
        List<StudentAssignmentResponse> content = assignments.subList(start, end).stream()
                .map(assignment -> toResponse(assignment, latestSubmissions.get(assignment.getId())))
                .toList();
        return new PageImpl<>(content, PageRequest.of(page, size), assignments.size());
    }

    private boolean matchesStatus(Assignment assignment, String requested, Submission submission) {
        String normalized = requested.trim().toUpperCase();
        if (normalized.equals("OVERDUE")) {
            return submission == null && assignment.getDueDate().isBefore(LocalDateTime.now());
        }
        if (normalized.equals("UPCOMING")) {
            return assignment.getDueDate().isAfter(LocalDateTime.now());
        }
        if (normalized.equals("NOT_SUBMITTED")) {
            return submission == null;
        }
        return submission != null && submission.getStatus().name().equals(normalized);
    }

    private StudentAssignmentResponse toResponse(Assignment assignment, Submission submission) {
        return StudentAssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourseId())
                .moduleId(assignment.getModuleId())
                .lessonId(assignment.getLessonId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .instructions(assignment.getInstructions())
                .startDate(assignment.getStartDate())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks())
                .assignmentStatus(assignment.getStatus().name())
                .submissionId(submission == null ? null : submission.getId())
                .submissionStatus(submission == null ? null : submission.getStatus())
                .attemptNumber(submission == null ? null : submission.getAttemptNumber())
                .submittedAt(submission == null ? null : submission.getSubmittedAt())
                .marks(submission == null ? null : submission.getMarks())
                .feedback(submission == null ? null : submission.getFeedback())
                .build();
    }
}
