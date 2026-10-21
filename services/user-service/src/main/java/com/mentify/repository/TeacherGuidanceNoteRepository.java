package com.mentify.repository;

import com.mentify.entity.TeacherGuidanceNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TeacherGuidanceNoteRepository extends JpaRepository<TeacherGuidanceNote, UUID> {
    List<TeacherGuidanceNote> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
