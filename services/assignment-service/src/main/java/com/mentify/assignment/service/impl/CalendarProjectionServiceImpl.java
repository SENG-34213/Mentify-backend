package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.CalendarProjectionClient;
import com.mentify.assignment.client.dto.CalendarProjectionRequest;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.service.CalendarProjectionAttemptService;
import com.mentify.assignment.service.CalendarProjectionService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarProjectionServiceImpl implements CalendarProjectionService {

    private static final String SOURCE_ASSIGNMENT = "ASSIGNMENT";
    private static final String SOURCE_QUIZ = "QUIZ";
    private static final long DEADLINE_WINDOW_HOURS = 1;

    private final CalendarProjectionClient calendarClient;
    private final CalendarProjectionAttemptService attempts;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;

    @Value("${mentify.clients.calendar-service.internal-service-token:}")
    private String serviceToken;

    @Override
    public void projectAssignmentIfEligible(Assignment assignment, String authorizationHeader) {
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED || !assignment.isActive()) {
            return;
        }
        try {
            calendarClient.upsertAssignment(toAssignmentRequest(assignment), authorizationHeader,
                    "assignment-service", serviceToken);
            markSucceeded(SOURCE_ASSIGNMENT, assignment.getId());
        } catch (RuntimeException failure) {
            recordFailure(SOURCE_ASSIGNMENT, assignment.getId(), failure);
        }
    }

    @Override
    public void cancelAssignment(Assignment assignment, String authorizationHeader) {
        try {
            calendarClient.cancelAssignment(assignment.getId(), authorizationHeader,
                    "assignment-service", serviceToken);
            markSucceeded(SOURCE_ASSIGNMENT, assignment.getId());
        } catch (RuntimeException failure) {
            recordFailure(SOURCE_ASSIGNMENT, assignment.getId(), failure);
        }
    }

    @Override
    public void projectQuizIfEligible(Quiz quiz, String authorizationHeader) {
        if (quiz.getStatus() != QuizStatus.PUBLISHED || !quiz.isActive()
                || quiz.getStartTime() == null) {
            return;
        }
        try {
            calendarClient.upsertQuiz(toQuizRequest(quiz), authorizationHeader,
                    "assignment-service", serviceToken);
            markSucceeded(SOURCE_QUIZ, quiz.getId());
        } catch (RuntimeException failure) {
            recordFailure(SOURCE_QUIZ, quiz.getId(), failure);
        }
    }

    @Override
    public void cancelQuiz(Quiz quiz, String authorizationHeader) {
        try {
            calendarClient.cancelQuiz(quiz.getId(), authorizationHeader,
                    "assignment-service", serviceToken);
            markSucceeded(SOURCE_QUIZ, quiz.getId());
        } catch (RuntimeException failure) {
            recordFailure(SOURCE_QUIZ, quiz.getId(), failure);
        }
    }

    @Override
    public void reconcileAssignment(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED && assignment.isActive()) {
            projectAssignmentIfEligible(assignment, authorizationHeader);
        } else {
            cancelAssignment(assignment, authorizationHeader);
        }
    }

    @Override
    public void reconcileQuiz(UUID quizId, String authorizationHeader) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        if (quiz.getStatus() == QuizStatus.PUBLISHED && quiz.isActive()
                && quiz.getStartTime() != null) {
            projectQuizIfEligible(quiz, authorizationHeader);
        } else {
            cancelQuiz(quiz, authorizationHeader);
        }
    }

    private CalendarProjectionRequest toAssignmentRequest(Assignment assignment) {
        return CalendarProjectionRequest.builder()
                .sourceId(assignment.getId())
                .courseId(assignment.getCourseId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .startDateTime(assignment.getDueDate().minusHours(DEADLINE_WINDOW_HOURS))
                .endDateTime(assignment.getDueDate())
                .createdBy(assignment.getTeacherId())
                .build();
    }

    private CalendarProjectionRequest toQuizRequest(Quiz quiz) {
        LocalDateTime end = quiz.getEndTime() != null
                ? quiz.getEndTime()
                : quiz.getStartTime().plusMinutes(quiz.getDurationMinutes());
        return CalendarProjectionRequest.builder()
                .sourceId(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .startDateTime(quiz.getStartTime())
                .endDateTime(end)
                .createdBy(quiz.getTeacherId())
                .build();
    }

    private void markSucceeded(String sourceType, UUID sourceId) {
        try {
            attempts.succeeded(sourceType, sourceId);
        } catch (RuntimeException failure) {
            log.error("Calendar projection succeeded but outcome could not be recorded for {} {}",
                    sourceType, sourceId);
        }
    }

    private void recordFailure(String sourceType, UUID sourceId, RuntimeException failure) {
        if (failure instanceof FeignException feign) {
            log.warn("Calendar projection failed for {} {} with dependency status {}",
                    sourceType, sourceId, feign.status());
        } else {
            log.warn("Calendar projection failed for {} {}: {}", sourceType, sourceId,
                    failure.getClass().getSimpleName());
        }
        try {
            attempts.failed(sourceType, sourceId, failure);
        } catch (RuntimeException recordingFailure) {
            log.error("Calendar projection failure could not be recorded for {} {}", sourceType, sourceId);
        }
    }
}
