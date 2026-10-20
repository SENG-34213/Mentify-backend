package com.mentify.repository;

import com.mentify.entity.StudentLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentLessonProgressRepository extends JpaRepository<StudentLessonProgress, UUID> {
    Optional<StudentLessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);
    List<StudentLessonProgress> findAllByStudentIdOrderByLastAccessedAtDesc(UUID studentId);
    List<StudentLessonProgress> findAllByStudentIdAndCourseIdOrderByLastAccessedAtDesc(UUID studentId, UUID courseId);
}
