package com.mentify.quiz.repository;

import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.enums.AttemptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndIsActiveTrue(UUID id);

    Optional<QuizAttempt> findByIdAndStudentIdAndIsActiveTrue(UUID id, UUID studentId);

    long countByQuizIdAndStudentIdAndIsActiveTrue(UUID quizId, UUID studentId);

    boolean existsByQuizIdAndStudentIdAndStatusAndIsActiveTrue(UUID quizId, UUID studentId, AttemptStatus status);

    @EntityGraph(attributePaths = "answers")
    Optional<QuizAttempt> findWithAnswersByIdAndIsActiveTrue(UUID id);
}
