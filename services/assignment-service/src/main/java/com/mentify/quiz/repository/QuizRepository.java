package com.mentify.quiz.repository;

import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.enums.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByIdAndIsActiveTrue(UUID id);

    List<Quiz> findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(UUID teacherId);

    List<Quiz> findByCourseIdAndStatusAndIsActiveTrueOrderByStartTimeAsc(UUID courseId, QuizStatus status);

    List<Quiz> findByStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            QuizStatus status
    );

    List<Quiz> findByTeacherIdAndStartTimeBetweenAndStatusAndIsActiveTrueOrderByStartTimeAsc(
            UUID teacherId,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            QuizStatus status
    );
}
