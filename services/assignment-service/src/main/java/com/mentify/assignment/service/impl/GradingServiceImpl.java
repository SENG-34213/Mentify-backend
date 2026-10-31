package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.GradingService;
import com.mentify.assignment.service.SubmissionResponseMapper;
import com.mentify.common.notification.NotificationEventPublisher;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final SubmissionRepository submissionRepository;
    private final CurrentUserService currentUserService;
    private final SubmissionResponseMapper submissionResponseMapper;

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    @Autowired(required = false)
    private CourseServiceClient courseServiceClient;

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> gradeSubmission(UUID submissionId, GradeSubmissionRequest request, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);

        BigDecimal marks = request.getMarks();
        if (marks == null || marks.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Marks are required and cannot be negative");
        }

        Assignment assignment = submission.getAssignment();
        if (marks.compareTo(BigDecimal.valueOf(assignment.getMaxMarks())) > 0) {
            throw new RuntimeException("Marks cannot exceed assignment maximum");
        }
        BigDecimal finalMarks = marks;
        if (Boolean.TRUE.equals(submission.getIsLate())
                && assignment.getLatePenaltyPercentage() != null
                && assignment.getLatePenaltyPercentage() > 0) {
            BigDecimal penalty = marks.multiply(BigDecimal.valueOf(assignment.getLatePenaltyPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            finalMarks = marks.subtract(penalty).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        submission.setMarks(finalMarks);
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(currentUserService.getCurrentUserId());
        submission.setGradeReleased(false);

        Submission saved = submissionRepository.save(submission);
        String gradingDetails = "Your teacher graded \"" + assignment.getTitle() + "\" for "
                + courseDetails(assignment, authorizationHeader) + ". Marks: "
                + saved.getMarks() + " / " + assignment.getMaxMarks() + "; due: "
                + assignment.getDueDate() + "; submitted: " + saved.getSubmittedAt()
                + "; graded: " + saved.getGradedAt() + "; attempt: " + saved.getAttemptNumber()
                + "; status: " + saved.getStatus() + ".";
        if (saved.getFeedback() != null && !saved.getFeedback().isBlank()) {
            gradingDetails += " Teacher feedback: " + saved.getFeedback();
        }
        return response(HttpStatus.OK, "Grade draft saved successfully",
                submissionResponseMapper.toResponse(saved, authorizationHeader));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> returnSubmission(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);
        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setReturnedAt(LocalDateTime.now());
        submission.setGradeReleased(true);
        Submission saved = submissionRepository.save(submission);
        Assignment assignment = saved.getAssignment();
        publishStudentNotification(saved.getStudentId(), "SUBMISSION_RETURNED", "Assignment returned",
                "Your teacher returned \"" + assignment.getTitle() + "\" for "
                        + courseDetails(assignment, authorizationHeader) + " on " + saved.getReturnedAt()
                        + ". Assignment due date: " + assignment.getDueDate() + "; attempt: "
                        + saved.getAttemptNumber() + "; status: " + saved.getStatus()
                        + (saved.getFeedback() == null || saved.getFeedback().isBlank()
                        ? ". Review the assignment page for requested changes."
                        : "; teacher feedback: " + saved.getFeedback()));
        return response(HttpStatus.OK, "Grade released successfully",
                submissionResponseMapper.toResponse(saved, authorizationHeader));
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionResponse> holdSubmissionGrade(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        ensureTeacherCanGrade(submission);
        submission.setGradeReleased(false);
        submission.setReturnedAt(null);
        submission.setStatus(submission.getMarks() == null ? SubmissionStatus.SUBMITTED : SubmissionStatus.GRADED);
        Submission saved = submissionRepository.save(submission);
        return response(HttpStatus.OK, "Grade held successfully",
                submissionResponseMapper.toResponse(saved, authorizationHeader));
    }

    private void publishStudentNotification(UUID studentId, String type, String title, String message) {
        if (notificationEventPublisher != null && studentId != null) {
            notificationEventPublisher.publishForRecipient(studentId.toString(), null, type, title, message);
        }
    }

    private String courseDetails(Assignment assignment, String authorizationHeader) {
        try {
            if (courseServiceClient != null) {
                ApiResponse<CourseLookupResponse> response = courseServiceClient.getCourseById(
                        assignment.getCourseId(), authorizationHeader);
                CourseLookupResponse course = response == null ? null : response.getData();
                if (course != null && course.getCourseName() != null && !course.getCourseName().isBlank()) {
                    return course.getCourseName() + (course.getCourseCode() == null
                            || course.getCourseCode().isBlank() ? "" : " (" + course.getCourseCode() + ")");
                }
            }
        } catch (RuntimeException ignored) {
            // Keep grading successful if the optional notification detail lookup fails.
        }
        return "course details unavailable";
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
