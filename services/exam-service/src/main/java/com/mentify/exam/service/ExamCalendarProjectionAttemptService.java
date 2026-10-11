package com.mentify.exam.service;

import com.mentify.exam.entity.ExamCalendarProjectionAttempt;
import com.mentify.exam.repository.ExamCalendarProjectionAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamCalendarProjectionAttemptService {

    private final ExamCalendarProjectionAttemptRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeeded(UUID examId) {
        ExamCalendarProjectionAttempt attempt = repository.findByExamId(examId)
                .orElseGet(() -> ExamCalendarProjectionAttempt.builder()
                        .examId(examId).attempts(0).build());
        attempt.setState("SUCCEEDED");
        attempt.setLastError(null);
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID examId, Exception failure) {
        ExamCalendarProjectionAttempt attempt = repository.findByExamId(examId)
                .orElseGet(() -> ExamCalendarProjectionAttempt.builder()
                        .examId(examId).attempts(0).build());
        attempt.setState("FAILED");
        attempt.setAttempts(attempt.getAttempts() + 1);
        // Keep persisted diagnostics safe: exception messages may contain URLs or configuration details.
        attempt.setLastError(failure.getClass().getSimpleName());
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }
}
