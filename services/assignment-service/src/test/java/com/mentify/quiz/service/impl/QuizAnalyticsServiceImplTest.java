package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.response.TodayQuizPerformanceResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAnalyticsServiceImplTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private CurrentUserService currentUserService;

    private QuizAnalyticsServiceImpl quizAnalyticsService;

    @BeforeEach
    void setUp() {
        quizAnalyticsService = new QuizAnalyticsServiceImpl(
                quizRepository,
                quizAttemptRepository,
                currentUserService
        );
    }

    @Test
    void teacherReceivesOnlyOwnTodayQuizPerformance() {
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        Quiz quiz = quiz(quizId, teacherId, "Java OOP Quiz");

        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(quizRepository.findByTeacherIdAndStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                eq(teacherId),
                any(),
                any(),
                eq(QuizStatus.PUBLISHED)
        )).thenReturn(List.of(quiz));
        when(quizAttemptRepository.findByQuizIdInAndStatusAndIsActiveTrue(List.of(quizId), AttemptStatus.SUBMITTED))
                .thenReturn(List.of(
                        attempt(quizId, UUID.randomUUID(), "80.00", "8.00"),
                        attempt(quizId, UUID.randomUUID(), "60.00", "6.00")
                ));

        TodayQuizPerformanceResponse response = quizAnalyticsService.getTodayQuizPerformance();

        assertThat(response.getResultType()).isEqualTo("SINGLE_QUIZ");
        assertThat(response.getQuizzes()).hasSize(1);
        assertThat(response.getQuizzes().get(0).getAveragePercentage()).isEqualByComparingTo("70.00");
        assertThat(response.getQuizzes().get(0).getHighestPercentage()).isEqualByComparingTo("80.00");
        assertThat(response.getQuizzes().get(0).getLowestPercentage()).isEqualByComparingTo("60.00");
        assertThat(response.getQuizzes().get(0).getPassedAttempts()).isEqualTo(2);
        assertThat(response.getQuizzes().get(0).getBelowPassThresholdAttempts()).isZero();

        verify(quizRepository, never()).findByStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                any(),
                any(),
                any()
        );
    }

    @Test
    void adminReceivesGlobalTodayQuizPerformance() {
        Quiz quiz = quiz(UUID.randomUUID(), UUID.randomUUID(), "Database Quiz");

        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(quizRepository.findByStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                any(),
                any(),
                eq(QuizStatus.PUBLISHED)
        )).thenReturn(List.of(quiz));
        when(quizAttemptRepository.findByQuizIdInAndStatusAndIsActiveTrue(List.of(quiz.getId()), AttemptStatus.SUBMITTED))
                .thenReturn(List.of());

        TodayQuizPerformanceResponse response = quizAnalyticsService.getTodayQuizPerformance();

        assertThat(response.getResultType()).isEqualTo("SINGLE_QUIZ");
        assertThat(response.getQuizzes().get(0).getSubmittedAttempts()).isZero();
        verify(currentUserService, never()).getCurrentUserId();
    }

    @Test
    void noTodayQuizzesReturnsNoDataResult() {
        UUID teacherId = UUID.randomUUID();

        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(quizRepository.findByTeacherIdAndStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                eq(teacherId),
                any(),
                any(),
                eq(QuizStatus.PUBLISHED)
        )).thenReturn(List.of());

        TodayQuizPerformanceResponse response = quizAnalyticsService.getTodayQuizPerformance();

        assertThat(response.getResultType()).isEqualTo("NO_QUIZZES");
        assertThat(response.getQuizzes()).isEmpty();
    }

    @Test
    void multipleTodayQuizzesReturnsAmbiguousResultType() {
        UUID teacherId = UUID.randomUUID();
        Quiz firstQuiz = quiz(UUID.randomUUID(), teacherId, "Java Quiz");
        Quiz secondQuiz = quiz(UUID.randomUUID(), teacherId, "SQL Quiz");

        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(quizRepository.findByTeacherIdAndStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
                eq(teacherId),
                any(),
                any(),
                eq(QuizStatus.PUBLISHED)
        )).thenReturn(List.of(firstQuiz, secondQuiz));
        when(quizAttemptRepository.findByQuizIdInAndStatusAndIsActiveTrue(
                List.of(firstQuiz.getId(), secondQuiz.getId()),
                AttemptStatus.SUBMITTED
        )).thenReturn(List.of());

        TodayQuizPerformanceResponse response = quizAnalyticsService.getTodayQuizPerformance();

        assertThat(response.getResultType()).isEqualTo("MULTIPLE_QUIZZES");
        assertThat(response.getQuizzes()).extracting("quizTitle").containsExactly("Java Quiz", "SQL Quiz");
    }

    private Quiz quiz(UUID quizId, UUID teacherId, String title) {
        Quiz quiz = Quiz.builder()
                .courseId(UUID.randomUUID())
                .teacherId(teacherId)
                .title(title)
                .durationMinutes(30)
                .totalMarks(new BigDecimal("10.00"))
                .passMark(new BigDecimal("5.00"))
                .startTime(LocalDate.now().atTime(9, 0))
                .maxAttempts(1)
                .status(QuizStatus.PUBLISHED)
                .build();
        quiz.setId(quizId);
        return quiz;
    }

    private QuizAttempt attempt(UUID quizId, UUID studentId, String percentage, String score) {
        return QuizAttempt.builder()
                .quizId(quizId)
                .studentId(studentId)
                .attemptNumber(1)
                .startedAt(LocalDate.now().atTime(9, 5))
                .submittedAt(LocalDate.now().atTime(9, 20))
                .percentage(new BigDecimal(percentage))
                .score(new BigDecimal(score))
                .status(AttemptStatus.SUBMITTED)
                .build();
    }
}
