package com.mentify.exam.repository;

import com.mentify.exam.entity.ExamAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttachmentRepository extends JpaRepository<ExamAttachment, UUID> {
    List<ExamAttachment> findByExamIdAndActiveTrueOrderByCreatedAtAsc(UUID examId);
    Optional<ExamAttachment> findByIdAndExamIdAndActiveTrue(UUID id, UUID examId);
}
