package com.mentify.repository;

import com.mentify.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByIdAndModule_Id(UUID id, UUID moduleId);

    List<Lesson> findAllByModule_Id(UUID moduleId);

    boolean existsByModule_Id(UUID moduleId);
}
