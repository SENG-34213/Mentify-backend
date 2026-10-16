package com.mentify.attendance.repository;

import com.mentify.attendance.entity.CalendarProjectionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalendarProjectionAttemptRepository extends JpaRepository<CalendarProjectionAttempt, UUID> {
    Optional<CalendarProjectionAttempt> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);
}
