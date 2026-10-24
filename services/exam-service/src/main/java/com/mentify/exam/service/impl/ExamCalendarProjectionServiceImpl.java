package com.mentify.exam.service.impl;

import com.mentify.exam.client.CalendarProjectionClient;
import com.mentify.exam.client.dto.ExamCalendarProjectionRequest;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.service.ExamCalendarProjectionAttemptService;
import com.mentify.exam.service.ExamCalendarProjectionService;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamCalendarProjectionServiceImpl implements ExamCalendarProjectionService {

    private final CalendarProjectionClient calendarClient;
    private final ExamCalendarProjectionAttemptService attempts;
    private final ExamRepository examRepository;

    @Value("${mentify.clients.calendar-service.internal-service-token:}")
    private String serviceToken;

    @Override
    public void projectIfEligible(Exam exam, String authorizationHeader) {
        if (exam.getStatus() != ExamStatus.SCHEDULED) {
            return;
        }
        try {
            calendarClient.upsert(toRequest(exam), authorizationHeader,
                    "exam-service", serviceToken);
            markSucceeded(exam.getId());
        } catch (RuntimeException failure) {
            recordFailure(exam.getId(), failure);
        }
    }

    @Override
    public void cancel(Exam exam, String authorizationHeader) {
        try {
            calendarClient.cancel(exam.getId(), authorizationHeader,
                    "exam-service", serviceToken);
            markSucceeded(exam.getId());
        } catch (RuntimeException failure) {
            recordFailure(exam.getId(), failure);
        }
    }

    @Override
    public void reconcile(UUID examId, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        if (exam.getStatus() == ExamStatus.CANCELLED) {
            cancel(exam, authorizationHeader);
        } else if (exam.getStatus() == ExamStatus.SCHEDULED) {
            projectIfEligible(exam, authorizationHeader);
        } else {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Only scheduled or cancelled exams can be reconciled to Calendar");
        }
    }

    private ExamCalendarProjectionRequest toRequest(Exam exam) {
        return ExamCalendarProjectionRequest.builder()
                .examId(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .venue(exam.getVenue())
                .startDateTime(LocalDateTime.of(exam.getExamDate(), exam.getStartTime()))
                .endDateTime(LocalDateTime.of(exam.getExamDate(), exam.getEndTime()))
                .createdBy(exam.getCreatedBy())
                .build();
    }

    private void markSucceeded(UUID examId) {
        try {
            attempts.succeeded(examId);
        } catch (RuntimeException failure) {
            log.error("Calendar projection succeeded but outcome could not be recorded for exam {}", examId);
        }
    }

    private void recordFailure(UUID examId, RuntimeException failure) {
        if (failure instanceof FeignException feign) {
            log.warn("Calendar projection failed for exam {} with dependency status {}", examId, feign.status());
        } else {
            log.warn("Calendar projection failed for exam {}: {}", examId,
                    failure.getClass().getSimpleName());
        }
        try {
            attempts.failed(examId, failure);
        } catch (RuntimeException recordingFailure) {
            log.error("Calendar projection failure could not be recorded for exam {}", examId);
        }
    }
}
