package com.mentify.repository;

import com.mentify.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

	Optional<LearningMaterial> findByIdAndModule_Id(UUID id, UUID moduleId);

	List<LearningMaterial> findAllByModule_Id(UUID moduleId);

	List<LearningMaterial> findAllByLesson_Id(UUID lessonId);

	@Query("""
			select lm.materialId
			from LearningMaterial lm
			where lm.module.course.id = :courseId
			  and lm.materialId is not null
			""")
	List<UUID> findMaterialIdsByCourseId(@Param("courseId") UUID courseId);

	@Query("""
			select lm.materialId
			from LearningMaterial lm
			where lm.module.id = :moduleId
			  and lm.materialId is not null
			""")
	List<UUID> findMaterialIdsByModuleId(@Param("moduleId") UUID moduleId);

	@Query("""
			select lm.materialId
			from LearningMaterial lm
			where lm.lesson.id = :lessonId
			  and lm.materialId is not null
			""")
	List<UUID> findMaterialIdsByLessonId(@Param("lessonId") UUID lessonId);

	boolean existsByModule_Id(UUID moduleId);

	boolean existsByLesson_Id(UUID lessonId);

}
