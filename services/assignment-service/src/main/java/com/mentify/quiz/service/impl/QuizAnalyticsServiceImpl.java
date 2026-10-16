package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.response.QuizPerformanceSummaryResponse;
import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
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
import java.util.stream.Collectors;

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
