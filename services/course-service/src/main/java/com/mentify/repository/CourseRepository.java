package com.mentify.repository;

import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCourseNameAndGradeId(String courseName, UUID gradeId);

    boolean existsByCourseNameAndGradeIdAndIdNot(String courseName, UUID gradeId, UUID id);

    boolean existsByGradeId(UUID gradeId);

    List<Course> findAllByAssignedTeacherIdOrderByCreatedAtDesc(UUID assignedTeacherId);

    long countByStatusNot(CourseStatus status);

    long countByCreatedAtAfter(LocalDateTime createdAt);

}
