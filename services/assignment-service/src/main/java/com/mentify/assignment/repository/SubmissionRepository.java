package com.mentify.assignment.repository;

import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    interface AssignmentSubmissionCounts {
        UUID getAssignmentId();
        Long getTotalSubmissions();
        Long getGradedSubmissions();
    }

    @Query("""
            SELECT s.assignment.id AS assignmentId,
                   COUNT(s.id) AS totalSubmissions,
                   SUM(CASE WHEN s.marks IS NOT NULL THEN 1 ELSE 0 END) AS gradedSubmissions
            FROM Submission s
            WHERE s.assignment.id IN :assignmentIds AND s.isActive = true
            GROUP BY s.assignment.id
            """)
    List<AssignmentSubmissionCounts> findSubmissionCountsByAssignmentIds(
            @Param("assignmentIds") List<UUID> assignmentIds
    );

    Optional<Submission> findByIdAndIsActiveTrue(UUID id);

    Optional<Submission> findByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    List<Submission> findByAssignmentIdAndIsActiveTrueOrderByAttemptNumberAsc(UUID assignmentId);

    List<Submission> findByStudentIdAndIsActiveTrueOrderBySubmittedAtDesc(UUID studentId);

    List<Submission> findByAssignmentIdInAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(
            List<UUID> assignmentIds, UUID studentId);

    long countByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    boolean existsByAssignmentIdAndStudentIdAndIsActiveTrue(UUID assignmentId, UUID studentId);

    Optional<Submission> findFirstByAssignmentIdAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(UUID assignmentId, UUID studentId);

    Optional<Submission> findByAssignmentIdAndStudentIdAndStatusAndIsActiveTrue(UUID assignmentId, UUID studentId, SubmissionStatus status);
}
