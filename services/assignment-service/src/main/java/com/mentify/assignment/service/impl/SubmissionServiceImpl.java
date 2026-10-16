package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateSubmissionRequest;
import com.mentify.assignment.dto.request.UpdateSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.SubmissionService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.UserServiceClient;
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
public class SubmissionServiceImpl implements SubmissionService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final UserServiceClient userServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> createSubmission(UUID assignmentId, CreateSubmissionRequest request, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        if (assignment.getStatus() != com.mentify.assignment.enums.AssignmentStatus.PUBLISHED) {
            throw new RuntimeException("Assignment is not published yet");
        }

        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!userServiceClient.isStudentExists(currentUserId, authorizationHeader)) {
            throw new RuntimeException("Student not found");
        }
        if (!enrollmentServiceClient.isStudentEnrolledInCourse(currentUserId, assignment.getCourseId(), authorizationHeader)) {
            throw new RuntimeException("Student is not enrolled in this course");
        }

        long existingAttempts = submissionRepository.countByAssignmentIdAndStudentIdAndIsActiveTrue(assignmentId, currentUserId);
        if (assignment.getAllowedAttempts() != null && existingAttempts >= assignment.getAllowedAttempts()) {
            throw new RuntimeException("Maximum submission attempts reached");
        }

        Submission submission = Submission.builder()
                .assignment(assignment)
                .studentId(currentUserId)
                .attemptNumber((int) existingAttempts + 1)
                .status(SubmissionStatus.DRAFT)
                .content(request == null ? null : request.getContent())
                .isLate(false)
                .build();

        Submission saved = submissionRepository.save(submission);
        return response(HttpStatus.CREATED, "Submission created successfully", mapToResponse(saved));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> updateSubmission(UUID submissionId, UpdateSubmissionRequest request, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureCanAccessSubmission(submission);
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new RuntimeException("Submission can only be updated while in DRAFT status");
        }
        submission.setContent(request.getContent());
        return response(HttpStatus.OK, "Submission updated successfully", mapToResponse(submissionRepository.save(submission)));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> submitAssignment(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        Assignment assignment = submission.getAssignment();
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!currentUserId.equals(submission.getStudentId())) {
            throw new RuntimeException("You can only submit your own assignment");
        }
        if (assignment.getStatus() != com.mentify.assignment.enums.AssignmentStatus.PUBLISHED) {
            throw new RuntimeException("Assignment is not open for submission");
        }

        if (LocalDateTime.now().isAfter(assignment.getDueDate()) && !Boolean.TRUE.equals(assignment.getAllowLateSubmission())) {
            throw new RuntimeException("Late submission is not allowed for this assignment");
        }

        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new RuntimeException("Submission has already been submitted");
        }

        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setIsLate(LocalDateTime.now().isAfter(assignment.getDueDate()));

        return response(HttpStatus.OK, "Assignment submitted successfully", mapToResponse(submissionRepository.save(submission)));
    }

    @Override
    public ApiResponse<SubmissionResponse> getSubmission(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureCanAccessSubmission(submission);
        return response(HttpStatus.OK, "Submission fetched successfully", mapToResponse(submission));
    }

    @Override
    public ApiResponse<List<SubmissionResponse>> listSubmissionsForAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        if (!canTeacherAccess(assignment)) {
            throw new RuntimeException("Access denied");
        }
        List<SubmissionResponse> submissions = submissionRepository.findByAssignmentIdAndIsActiveTrueOrderByAttemptNumberAsc(assignmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return response(HttpStatus.OK, "Submissions fetched successfully", submissions);
    }

    @Override
    public ApiResponse<List<SubmissionResponse>> listMySubmissions(String authorizationHeader) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<SubmissionResponse> submissions = submissionRepository.findByStudentIdAndIsActiveTrueOrderBySubmittedAtDesc(currentUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return response(HttpStatus.OK, "Your submissions fetched successfully", submissions);
    }

    private Assignment getAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findByIdAndIsActiveTrue(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
    }

    private Submission getSubmissionOrThrow(UUID submissionId) {
        return submissionRepository.findByIdAndIsActiveTrue(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    private void ensureCanAccessSubmission(Submission submission) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (currentUserService.hasAnyRole("TEACHER") && currentUserService.getCurrentUserId().equals(submission.getAssignment().getTeacherId())) {
            return;
        }
        if (currentUserId.equals(submission.getStudentId())) {
            return;
        }
        throw new RuntimeException("Access denied");
    }

    private boolean canTeacherAccess(Assignment assignment) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return true;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            return false;
        }
        return currentUserService.getCurrentUserId().equals(assignment.getTeacherId());
    }

    private SubmissionResponse mapToResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment() != null ? submission.getAssignment().getId() : null)
                .studentId(submission.getStudentId())
                .attemptNumber(submission.getAttemptNumber())
                .status(submission.getStatus())
                .content(submission.getContent())
                .isLate(submission.getIsLate())
                .submittedAt(submission.getSubmittedAt())
                .marks(submission.getMarks())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .gradedBy(submission.getGradedBy())
                .returnedAt(submission.getReturnedAt())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
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
