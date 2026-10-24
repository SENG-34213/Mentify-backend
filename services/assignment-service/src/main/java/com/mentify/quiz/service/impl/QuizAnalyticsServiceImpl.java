package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.response.QuizPerformanceSummaryResponse;
import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.dto.response.QuizAnalyticsSummaryResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import com.mentify.quiz.service.QuizAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.RoundingMode;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAnalyticsServiceImpl implements QuizAnalyticsService {

    private static final String RESULT_NO_QUIZZES = "NO_QUIZZES";
    private static final String RESULT_SINGLE_QUIZ = "SINGLE_QUIZ";
    private static final String RESULT_MULTIPLE_QUIZZES = "MULTIPLE_QUIZZES";

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CurrentUserService currentUserService;
    private final CourseServiceClient courseServiceClient;

    @Override
    @Transactional(readOnly = true)
    public TodayQuizPerformanceResponse getTodayQuizPerformance() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Quiz> quizzes = findVisibleQuizzes(startOfDay, endOfDay);
        List<QuizPerformanceSummaryResponse> summaries = buildSummaries(quizzes);

        return TodayQuizPerformanceResponse.builder()
                .date(today)
                .resultType(resolveResultType(summaries.size()))
                .quizzes(summaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAnalyticsSummaryResponse getOverview() {
        return summarize(null);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAnalyticsSummaryResponse getCourseSummary(UUID courseId) {
        return summarize(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAnalyticsSummaryResponse getOverview(LocalDate from, LocalDate to) {
        return summarize(null, normalizeDateRange(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAnalyticsSummaryResponse getCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        assertTeacherCanAccess(course);
        return summarize(courseId, normalizeDateRange(from, to));
    }

    private QuizAnalyticsSummaryResponse summarize(UUID courseId) {
        return summarize(courseId, new DateRange(null, null));
    }

    private QuizAnalyticsSummaryResponse summarize(UUID courseId, DateRange range) {
        List<Quiz> quizzes = quizRepository.findAll().stream()
                .filter(Quiz::isActive)
                .filter(quiz -> quiz.getStatus() == QuizStatus.CLOSED)
                .filter(quiz -> courseId == null || courseId.equals(quiz.getCourseId()))
                .filter(quiz -> range.includes(quiz.getStartTime()))
                .toList();
        List<UUID> quizIds = quizzes.stream().map(Quiz::getId).toList();
        List<QuizAttempt> attempts = quizAttemptRepository.findAll().stream()
                .filter(QuizAttempt::isActive)
                .filter(attempt -> quizIds.contains(attempt.getQuizId()))
                .toList();
        List<QuizAttempt> submitted = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .toList();
        BigDecimal completionRate = percentage(submitted.size(), attempts.size());
        BigDecimal missingRate = BigDecimal.valueOf(100).subtract(completionRate);
        List<BigDecimal> percentages = submitted.stream().map(QuizAttempt::getPercentage)
                .filter(value -> value != null).toList();
        BigDecimal average = percentages.isEmpty() ? BigDecimal.ZERO : percentages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(percentages.size()), 2, RoundingMode.HALF_UP);
        return new QuizAnalyticsSummaryResponse(courseId, completionRate, average, missingRate);
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
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
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
            throw new IllegalArgumentException("Quiz date 'from' must not be after 'to'");
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

    private List<Quiz> findVisibleQuizzes(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return quizRepository.findByStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                    startOfDay,
                    endOfDay,
                    QuizStatus.PUBLISHED
            );
        }

        UUID teacherId = currentUserService.getCurrentUserId();
        return quizRepository.findByTeacherIdAndStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                teacherId,
                startOfDay,
                endOfDay,
                QuizStatus.PUBLISHED
        );
    }

    private List<QuizPerformanceSummaryResponse> buildSummaries(List<Quiz> quizzes) {
        if (quizzes.isEmpty()) {
            return List.of();
        }

        List<UUID> quizIds = quizzes.stream().map(Quiz::getId).toList();
        Map<UUID, List<QuizAttempt>> attemptsByQuizId = quizAttemptRepository
                .findByQuizIdInAndStatusAndIsActiveTrue(quizIds, AttemptStatus.SUBMITTED)
                .stream()
                .collect(Collectors.groupingBy(QuizAttempt::getQuizId));

        return quizzes.stream()
                .map(quiz -> summarizeQuiz(quiz, attemptsByQuizId.getOrDefault(quiz.getId(), List.of())))
                .toList();
    }

    private QuizPerformanceSummaryResponse summarizeQuiz(Quiz quiz, List<QuizAttempt> attempts) {
        long submittedAttempts = attempts.size();
        long participants = attempts.stream()
                .map(QuizAttempt::getStudentId)
                .distinct()
                .count();
        long passedAttempts = attempts.stream()
                .filter(attempt -> isPassed(quiz, attempt))
                .count();
        long belowPassThresholdAttempts = submittedAttempts - passedAttempts;

        return QuizPerformanceSummaryResponse.builder()
                .quizId(quiz.getId())
                .courseId(quiz.getCourseId())
                .quizTitle(quiz.getTitle())
                .startTime(quiz.getStartTime())
                .totalMarks(quiz.getTotalMarks())
                .passMark(quiz.getPassMark())
                .submittedAttempts(submittedAttempts)
                .participants(participants)
                .averagePercentage(averagePercentage(attempts))
                .highestPercentage(extremePercentage(attempts, true))
                .lowestPercentage(extremePercentage(attempts, false))
                .passedAttempts(passedAttempts)
                .belowPassThresholdAttempts(belowPassThresholdAttempts)
                .build();
    }

    private boolean isPassed(Quiz quiz, QuizAttempt attempt) {
        return quiz.getPassMark() != null
                && attempt.getScore() != null
                && attempt.getScore().compareTo(quiz.getPassMark()) >= 0;
    }

    private BigDecimal averagePercentage(List<QuizAttempt> attempts) {
        List<BigDecimal> percentages = nonNullPercentages(attempts);
        if (percentages.isEmpty()) {
            return null;
        }

        BigDecimal total = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(percentages.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal extremePercentage(List<QuizAttempt> attempts, boolean highest) {
        return nonNullPercentages(attempts).stream()
                .reduce((left, right) -> highest ? left.max(right) : left.min(right))
                .orElse(null);
    }

    private List<BigDecimal> nonNullPercentages(List<QuizAttempt> attempts) {
        return attempts.stream()
                .map(QuizAttempt::getPercentage)
                .filter(percentage -> percentage != null)
                .toList();
    }

    private String resolveResultType(int quizCount) {
        if (quizCount == 0) {
            return RESULT_NO_QUIZZES;
        }
        if (quizCount == 1) {
            return RESULT_SINGLE_QUIZ;
        }
        return RESULT_MULTIPLE_QUIZZES;
    }
}
