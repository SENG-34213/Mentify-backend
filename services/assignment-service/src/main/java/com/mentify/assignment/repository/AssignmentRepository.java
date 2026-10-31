package com.mentify.assignment.repository;

import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndIsActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Assignment a WHERE a.id = :id AND a.isActive = true")
    Optional<Assignment> findByIdAndIsActiveTrueForUpdate(@Param("id") UUID id);

    List<Assignment> findByCourseIdAndIsActiveTrueOrderByStartDateDesc(UUID courseId);

    List<Assignment> findByCourseIdInAndIsActiveTrueOrderByDueDateAsc(List<UUID> courseIds);

    List<Assignment> findByCourseIdAndStatusAndIsActiveTrueOrderByDueDateAsc(UUID courseId, AssignmentStatus status);

    List<Assignment> findByModuleIdAndIsActiveTrueOrderByStartDateDesc(UUID moduleId);

    List<Assignment> findByLessonIdAndIsActiveTrueOrderByDueDateAsc(UUID lessonId);
}
