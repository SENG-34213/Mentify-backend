package com.mentify.assignment.repository;

import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndIsActiveTrue(UUID id);

    Optional<Submission> findByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    List<Submission> findByAssignmentIdAndIsActiveTrueOrderByAttemptNumberAsc(UUID assignmentId);

    List<Submission> findByStudentIdAndIsActiveTrueOrderBySubmittedAtDesc(UUID studentId);

    long countByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    boolean existsByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(UUID assignmentId, UUID studentId);

    Optional<Submission> findByAssignmentIdAndStudentIdAndStatusAndIsActiveTrue(UUID assignmentId, UUID studentId, SubmissionStatus status);
}
