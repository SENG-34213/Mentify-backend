package com.mentify.exam.service.impl;

import com.mentify.exam.client.CalendarProjectionClient;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.service.ExamCalendarProjectionAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamCalendarProjectionServiceImplTest {

    @Mock CalendarProjectionClient calendarClient;
    @Mock ExamCalendarProjectionAttemptService attempts;
    @Mock ExamRepository examRepository;
    @InjectMocks ExamCalendarProjectionServiceImpl service;

    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "serviceToken", "integration-token");
    }

    @Test
    void scheduledExamIsProjectedWithFailureIsolatedFromExamOperation() {
        Exam exam = exam(ExamStatus.SCHEDULED);
        when(calendarClient.upsert(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("calendar unavailable"));

        service.projectIfEligible(exam, "Bearer user-token");

        verify(attempts).failed(eq(examId), any(RuntimeException.class));
    }

    @Test
    void draftExamIsNotProjected() {
        service.projectIfEligible(exam(ExamStatus.DRAFT), "Bearer user-token");

        verify(calendarClient, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void cancelledExamUsesSourceCancellation() {
        service.cancel(exam(ExamStatus.CANCELLED), "Bearer user-token");

        verify(calendarClient).cancel(examId, "Bearer user-token", "exam-service", "integration-token");
    }

    private Exam exam(ExamStatus status) {
        return Exam.builder().id(examId).courseId(UUID.randomUUID()).title("Exam")
                .description("Description").examDate(LocalDate.of(2026, 10, 12))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .totalMarks(BigDecimal.valueOf(100)).passMarks(BigDecimal.valueOf(50))
                .status(status).createdBy(UUID.randomUUID()).build();
    }
}
