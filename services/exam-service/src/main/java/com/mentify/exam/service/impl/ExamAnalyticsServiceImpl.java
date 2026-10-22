package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamAnalyticsSummaryResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.ExamAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamAnalyticsServiceImpl implements ExamAnalyticsService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;

    @Override
    @Transactional(readOnly = true)
    public ExamAnalyticsSummaryResponse getOverview() {
        return summarize(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAnalyticsSummaryResponse getCourseSummary(UUID courseId) {
        return summarize(courseId);
    }

    private ExamAnalyticsSummaryResponse summarize(UUID courseId) {
        List<Exam> exams = examRepository.findAll().stream()
                .filter(exam -> exam.getStatus() == ExamStatus.COMPLETED)
                .filter(exam -> courseId == null || courseId.equals(exam.getCourseId()))
                .toList();
        List<UUID> examIds = exams.stream().map(Exam::getId).toList();
        List<ExamResult> results = examResultRepository.findAll().stream()
                .filter(result -> examIds.contains(result.getExamId()))
                .toList();
        long present = results.stream()
                .filter(result -> result.getAttendanceStatus() == AttendanceStatus.PRESENT).count();
        long absent = results.stream()
                .filter(result -> result.getAttendanceStatus() == AttendanceStatus.ABSENT).count();
        BigDecimal average = averagePercentage(results, exams);
        return new ExamAnalyticsSummaryResponse(courseId, percentage(present, results.size()), average,
                percentage(absent, results.size()));
    }

    private BigDecimal averagePercentage(List<ExamResult> results, List<Exam> exams) {
        List<BigDecimal> percentages = results.stream()
                .filter(result -> result.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .filter(result -> result.getMarksObtained() != null)
                .map(result -> exams.stream().filter(exam -> exam.getId().equals(result.getExamId())).findFirst()
                        .map(exam -> exam.getTotalMarks().signum() == 0 ? BigDecimal.ZERO
                                : result.getMarksObtained().multiply(BigDecimal.valueOf(100))
                                .divide(exam.getTotalMarks(), 2, RoundingMode.HALF_UP))
                        .orElse(BigDecimal.ZERO))
                .toList();
        return percentages.isEmpty() ? BigDecimal.ZERO : percentages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(percentages.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }
}
