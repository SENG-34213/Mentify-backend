package com.mentify.quiz.repository;

import com.mentify.quiz.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {

    Optional<StudentAnswer> findByAttempt_IdAndQuestionIdAndIsActiveTrue(UUID attemptId, UUID questionId);

    List<StudentAnswer> findByAttempt_IdAndIsActiveTrue(UUID attemptId);
}
