package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.GradingService;
import com.mentify.assignment.service.SubmissionResponseMapper;
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
    private final SubmissionResponseMapper submissionResponseMapper;

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

        return response(HttpStatus.OK, "Submission graded successfully",
                submissionResponseMapper.toResponse(submissionRepository.save(submission), authorizationHeader));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> returnSubmission(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setReturnedAt(LocalDateTime.now());
        return response(HttpStatus.OK, "Submission returned successfully",
                submissionResponseMapper.toResponse(submissionRepository.save(submission), authorizationHeader));
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

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
