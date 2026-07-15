package com.mentify.repository;

import com.mentify.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(teacher_code FROM 'TIT-TCH-([0-9]+)$') AS INTEGER)), 0)
            FROM teacher_profiles
            WHERE teacher_code IS NOT NULL
            """, nativeQuery = true)
    int findLastTeacherNumber();
}
