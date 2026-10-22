package com.mentify.repository;

import com.mentify.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByIdAndModule_Id(UUID id, UUID moduleId);

    List<Lesson> findAllByModule_Id(UUID moduleId);

    List<Lesson> findAllByModule_Course_Id(UUID courseId);

    boolean existsByModule_Id(UUID moduleId);

    @Query("select l.module.course.id as courseId, count(l.id) as count "
            + "from Lesson l where l.module.course.id in :courseIds group by l.module.course.id")
    List<CourseCountProjection> countByCourseIds(@Param("courseIds") Set<UUID> courseIds);
}
