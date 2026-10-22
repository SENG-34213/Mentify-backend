package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.response.AssignmentAnalyticsSummaryResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.AssignmentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentAnalyticsServiceImpl implements AssignmentAnalyticsService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

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

    private AssignmentAnalyticsSummaryResponse summarize(UUID courseId) {
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(Assignment::isActive)
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.CLOSED)
                .filter(assignment -> courseId == null || courseId.equals(assignment.getCourseId()))
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

    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }
}
