package com.mentify.repository;

import com.mentify.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(student_id FROM 'TIT-[0-9]{2}-([0-9]+)$') AS INTEGER)), 0)
            FROM student_profiles
            WHERE grade = :grade
              AND student_id IS NOT NULL
            """, nativeQuery = true)
    int findLastStudentNumberByGrade(@Param("grade") String grade);
}
