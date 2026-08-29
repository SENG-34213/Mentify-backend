package com.mentify.repository;

import com.mentify.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    Optional<Module> findByIdAndCourse_Id(UUID id, UUID courseId);
}
