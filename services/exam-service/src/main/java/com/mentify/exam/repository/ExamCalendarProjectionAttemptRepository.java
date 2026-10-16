package com.mentify.exam.repository;

import com.mentify.exam.entity.ExamCalendarProjectionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamCalendarProjectionAttemptRepository extends JpaRepository<ExamCalendarProjectionAttempt, UUID> {
    Optional<ExamCalendarProjectionAttempt> findByExamId(UUID examId);
}
