package com.mentify.assignment.repository;

import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndIsActiveTrue(UUID id);

    List<Assignment> findByCourseIdAndStatusAndIsActiveTrueOrderByDueDateAsc(UUID courseId, AssignmentStatus status);
}
