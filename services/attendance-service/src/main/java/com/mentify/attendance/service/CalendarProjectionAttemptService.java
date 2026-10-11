package com.mentify.attendance.service;

import com.mentify.attendance.entity.CalendarProjectionAttempt;
import com.mentify.attendance.repository.CalendarProjectionAttemptRepository;
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
    public void succeeded(UUID sourceId) {
        CalendarProjectionAttempt attempt = findOrCreate(sourceId);
        attempt.setState("SUCCEEDED");
        attempt.setLastError(null);
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID sourceId, Exception failure) {
        CalendarProjectionAttempt attempt = findOrCreate(sourceId);
        attempt.setState("FAILED");
        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastError(failure.getClass().getSimpleName());
        attempt.setUpdatedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    private CalendarProjectionAttempt findOrCreate(UUID sourceId) {
        return repository.findBySourceTypeAndSourceId("COURSE_CLASS", sourceId)
                .orElseGet(() -> CalendarProjectionAttempt.builder()
                        .sourceType("COURSE_CLASS").sourceId(sourceId).attempts(0).build());
    }
}
