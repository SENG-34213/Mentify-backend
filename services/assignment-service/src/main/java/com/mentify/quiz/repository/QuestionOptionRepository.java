package com.mentify.quiz.repository;

import com.mentify.quiz.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

    Optional<QuestionOption> findByIdAndQuestion_IdAndIsActiveTrue(UUID id, UUID questionId);
}
