package com.mentify.repository;

import com.mentify.entity.Entrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntrollmentRepository extends JpaRepository<Entrollment, UUID> {

    List<Entrollment> findAllByStudentIdAndIsActiveTrue(UUID studentId);
}

