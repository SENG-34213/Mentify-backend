package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.CalendarProjectionClient;
import com.mentify.assignment.client.dto.CalendarProjectionRequest;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.CalendarProjectionAttempt;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.CalendarProjectionAttemptRepository;
import com.mentify.assignment.service.CalendarProjectionAttemptService;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarProjectionServiceImplTest {

    @Mock
    private CalendarProjectionClient calendarClient;
    @Mock
    private CalendarProjectionAttemptRepository attemptRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private QuizRepository quizRepository;

    private CalendarProjectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalendarProjectionServiceImpl(
                calendarClient,
                new CalendarProjectionAttemptService(attemptRepository),
                assignmentRepository,
                quizRepository
        );
        lenient().when(attemptRepository.findBySourceTypeAndSourceId(any(), any()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    void publishedAssignmentIsProjectedWithDeadlineWindow() {
        Assignment assignment = Assignment.builder()
                .courseId(UUID.randomUUID()).teacherId(UUID.randomUUID()).title("Essay")
                .dueDate(LocalDateTime.of(2026, 10, 15, 12, 0)).status(AssignmentStatus.PUBLISHED)
                .build();
        assignment.setId(UUID.randomUUID());
        assignment.setActive(true);

        service.projectAssignmentIfEligible(assignment, "Bearer token");

        ArgumentCaptor<CalendarProjectionRequest> captor = ArgumentCaptor.forClass(CalendarProjectionRequest.class);
        verify(calendarClient).upsertAssignment(captor.capture(), any(), any(), any());
        assertThat(captor.getValue().getEndDateTime()).isEqualTo(assignment.getDueDate());
        assertThat(captor.getValue().getStartDateTime()).isEqualTo(assignment.getDueDate().minusHours(1));
    }

    @Test
    void draftAssignmentIsNotProjected() {
        Assignment assignment = Assignment.builder().status(AssignmentStatus.DRAFT).build();
        assignment.setId(UUID.randomUUID());

        service.projectAssignmentIfEligible(assignment, "Bearer token");

        verify(calendarClient, never()).upsertAssignment(any(), any(), any(), any());
    }

    @Test
    void quizWithoutExplicitEndUsesDurationAsCalendarEnd() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 9, 0);
        Quiz quiz = Quiz.builder().courseId(UUID.randomUUID()).teacherId(UUID.randomUUID()).title("Quiz")
                .startTime(start).durationMinutes(45).status(QuizStatus.PUBLISHED).build();
        quiz.setId(UUID.randomUUID());
        quiz.setActive(true);

        service.projectQuizIfEligible(quiz, "Bearer token");

        ArgumentCaptor<CalendarProjectionRequest> captor = ArgumentCaptor.forClass(CalendarProjectionRequest.class);
        verify(calendarClient).upsertQuiz(captor.capture(), any(), any(), any());
        assertThat(captor.getValue().getStartDateTime()).isEqualTo(start);
        assertThat(captor.getValue().getEndDateTime()).isEqualTo(start.plusMinutes(45));
    }

    @Test
    void calendarFailureIsRecordedAndDoesNotEscapeSourceOperation() {
        Assignment assignment = Assignment.builder().courseId(UUID.randomUUID()).teacherId(UUID.randomUUID())
                .title("Essay").dueDate(LocalDateTime.now().plusDays(1)).status(AssignmentStatus.PUBLISHED).build();
        assignment.setId(UUID.randomUUID());
        assignment.setActive(true);
        when(calendarClient.upsertAssignment(any(), any(), any(), any())).thenThrow(new RuntimeException("dependency"));

        service.projectAssignmentIfEligible(assignment, "Bearer token");

        verify(attemptRepository).save(any(CalendarProjectionAttempt.class));
    }
}
