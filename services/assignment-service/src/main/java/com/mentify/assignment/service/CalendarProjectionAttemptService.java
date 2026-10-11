package com.mentify.assignment.service;

import com.mentify.assignment.entity.CalendarProjectionAttempt;
import com.mentify.assignment.repository.CalendarProjectionAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarProjectionAttemptService {

    private final CalendarProjectionAttemptRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeeded(String sourceType, UUID sourceId) {
        CalendarProjectionAttempt attempt = findOrCreate(sourceType, sourceId);
        attempt.setState("SUCCEEDED");
        attempt.setLastError(null);
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(String sourceType, UUID sourceId, Exception failure) {
        CalendarProjectionAttempt attempt = findOrCreate(sourceType, sourceId);
        attempt.setState("FAILED");
        attempt.setAttempts(attempt.getAttempts() + 1);
        // Exception messages can contain URLs or configuration details; persist only a safe type.
        attempt.setLastError(failure.getClass().getSimpleName());
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    private CalendarProjectionAttempt findOrCreate(String sourceType, UUID sourceId) {
        return repository.findBySourceTypeAndSourceId(sourceType, sourceId)
                .orElseGet(() -> CalendarProjectionAttempt.builder()
                        .sourceType(sourceType).sourceId(sourceId).attempts(0).build());
    }
}
