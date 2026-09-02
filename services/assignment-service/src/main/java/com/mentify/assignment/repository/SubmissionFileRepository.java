package com.mentify.assignment.repository;

import com.mentify.assignment.entity.SubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, UUID> {

    List<SubmissionFile> findBySubmissionIdAndIsActiveTrueOrderByCreatedAtAsc(UUID submissionId);

    Optional<SubmissionFile> findByIdAndSubmissionIdAndIsActiveTrue(UUID id, UUID submissionId);
}
