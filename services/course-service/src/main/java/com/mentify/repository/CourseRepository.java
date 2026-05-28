package com.mentify.repository;

import com.mentify.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCourseNameAndGradeId(String courseName, String gradeId);

    List<Course> findAllByTeacherId(String teacherId);

    List<Course> findAllByGradeId(String gradeId);

    List<Course> findAllByIsPublishedTrueAndIsVisibleTrue();
}