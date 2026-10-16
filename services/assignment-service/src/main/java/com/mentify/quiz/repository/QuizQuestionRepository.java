package com.mentify.quiz.repository;

import com.mentify.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    @EntityGraph(attributePaths = "options")
    Optional<QuizQuestion> findByIdAndIsActiveTrue(UUID id);

    @EntityGraph(attributePaths = "options")
    Optional<QuizQuestion> findByQuiz_IdAndIdAndIsActiveTrue(UUID quizId, UUID id);

    @EntityGraph(attributePaths = "options")
    List<QuizQuestion> findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(UUID quizId);

    long countByQuiz_IdAndIsActiveTrue(UUID quizId);
}
