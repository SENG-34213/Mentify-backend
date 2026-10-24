package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.response.AssignmentAnalyticsSummaryResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.AssignmentAnalyticsService;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssignmentAnalyticsServiceImpl implements AssignmentAnalyticsService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public AssignmentAnalyticsSummaryResponse getOverview() {
        return summarize(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentAnalyticsSummaryResponse getCourseSummary(UUID courseId) {
        return summarize(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentAnalyticsSummaryResponse getOverview(LocalDate from, LocalDate to) {
        DateRange range = normalizeDateRange(from, to);
        return summarize(null, range);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentAnalyticsSummaryResponse getCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        assertTeacherCanAccess(course);
        return summarize(courseId, normalizeDateRange(from, to));
    }

    private AssignmentAnalyticsSummaryResponse summarize(UUID courseId) {
        return summarize(courseId, new DateRange(null, null));
    }

    private AssignmentAnalyticsSummaryResponse summarize(UUID courseId, DateRange range) {
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(Assignment::isActive)
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.CLOSED)
                .filter(assignment -> courseId == null || courseId.equals(assignment.getCourseId()))
                .filter(assignment -> range.includes(assignment.getDueDate()))
                .toList();
        List<UUID> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        List<Submission> submissions = submissionRepository.findAll().stream()
                .filter(Submission::isActive)
                .filter(submission -> submission.getAssignment() != null
                        && assignmentIds.contains(submission.getAssignment().getId()))
                .toList();
        List<Submission> submitted = submissions.stream()
                .filter(submission -> submission.getStatus() != SubmissionStatus.DRAFT)
                .toList();
        List<Submission> graded = submitted.stream()
                .filter(submission -> submission.getMarks() != null
                        && submission.getAssignment().getMaxMarks() != null
                        && submission.getAssignment().getMaxMarks() > 0)
                .toList();

        BigDecimal completionRate = percentage(submitted.stream()
                .map(submission -> submission.getAssignment().getId()).distinct().count(), assignments.size());
        BigDecimal missingRate = BigDecimal.valueOf(100).subtract(completionRate);
        BigDecimal averageScore = graded.isEmpty() ? BigDecimal.ZERO : graded.stream()
                .map(submission -> BigDecimal.valueOf(submission.getMarks().doubleValue() * 100
                        / submission.getAssignment().getMaxMarks()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(graded.size()), 2, RoundingMode.HALF_UP);
        return new AssignmentAnalyticsSummaryResponse(courseId, completionRate, averageScore, missingRate);
    }

    private CourseLookupResponse getCourseOrThrow(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<CourseLookupResponse> response = courseServiceClient.getCourseById(courseId, authorizationHeader);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Course not found with id: " + courseId);
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
    }

    private void assertTeacherCanAccess(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")
                || !currentUserService.getCurrentUserId().equals(course.getAssignedTeacherId())) {
            throw new AccessDeniedException("Teacher can only access assigned course analytics");
        }
    }

    private DateRange normalizeDateRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) return new DateRange(null, null);
        LocalDate normalizedFrom = from == null ? to : from;
        LocalDate normalizedTo = to == null ? from : to;
        if (normalizedFrom.isAfter(normalizedTo)) {
            throw new IllegalArgumentException("Assignment date 'from' must not be after 'to'");
        }
        return new DateRange(normalizedFrom, normalizedTo);
    }

    private record DateRange(LocalDate from, LocalDate to) {
        private boolean includes(LocalDateTime value) {
            if (from == null && to == null) return true;
            LocalDate date = value == null ? null : value.toLocalDate();
            return date != null && !date.isBefore(from) && !date.isAfter(to);
        }
    }

    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }
}
