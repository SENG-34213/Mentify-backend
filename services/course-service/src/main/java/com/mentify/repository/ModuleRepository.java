package com.mentify.repository;

import com.mentify.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    Optional<Module> findByIdAndCourse_Id(UUID id, UUID courseId);

    List<Module> findAllByCourse_Id(UUID courseId);

    @Query("select m.course.id as courseId, count(m.id) as count "
            + "from Module m where m.course.id in :courseIds group by m.course.id")
    List<CourseCountProjection> countByCourseIds(@Param("courseIds") Set<UUID> courseIds);
}
