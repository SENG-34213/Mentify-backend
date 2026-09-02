package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.GradingService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.security.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final SubmissionRepository submissionRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> gradeSubmission(UUID submissionId, GradeSubmissionRequest request, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);

        BigDecimal marks = request.getMarks();
        if (marks == null || marks.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Marks are required and cannot be negative");
        }

        submission.setMarks(marks);
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(currentUserService.getCurrentUserId());

        return response(HttpStatus.OK, "Submission graded successfully", mapToResponse(submissionRepository.save(submission)));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> returnSubmission(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setReturnedAt(LocalDateTime.now());
        return response(HttpStatus.OK, "Submission returned successfully", mapToResponse(submissionRepository.save(submission)));
    }

    private void ensureTeacherCanGrade(Submission submission) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            throw new RuntimeException("Access denied");
        }
        Assignment assignment = submission.getAssignment();
        if (assignment == null || !currentUserService.getCurrentUserId().equals(assignment.getTeacherId())) {
            throw new RuntimeException("Teacher can only grade submissions for their own assignments");
        }
    }

    private Submission getSubmissionOrThrow(UUID submissionId) {
        return submissionRepository.findByIdAndIsActiveTrue(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
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
