package com.mentify.assignment.repository;

import com.mentify.assignment.entity.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, UUID> {

    List<AssignmentAttachment> findByAssignmentIdAndIsActiveTrueOrderByCreatedAtAsc(UUID assignmentId);

    Optional<AssignmentAttachment> findByIdAndAssignmentIdAndIsActiveTrue(UUID id, UUID assignmentId);
}
