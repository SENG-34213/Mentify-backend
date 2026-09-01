package com.mentify.repository;

import com.mentify.entity.Entrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntrollmentRepository extends JpaRepository<Entrollment, UUID> {

    List<Entrollment> findAllByStudentIdAndIsActiveTrue(UUID studentId);

    Optional<Entrollment> findByIdAndIsActiveTrue(UUID id);

    @Query("""
            select count(e) > 0
            from Entrollment e
            join e.courseIds courseId
            where e.studentId = :studentId
              and courseId = :courseId
              and e.isActive = true
            """)
    boolean existsActiveEnrollmentForStudentAndCourse(
            @Param("studentId") UUID studentId,
            @Param("courseId") UUID courseId
    );
}
