package com.mentify.repository;

import com.mentify.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

	Optional<LearningMaterial> findByIdAndModule_Id(UUID id, UUID moduleId);

	List<LearningMaterial> findAllByModule_Id(UUID moduleId);
}
