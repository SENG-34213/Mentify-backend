package com.mentify.repository;

import com.mentify.entity.GradeMonthlyFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeMonthlyFeeRepository extends JpaRepository<GradeMonthlyFee, UUID> {
    Optional<GradeMonthlyFee> findByGrade_Id(UUID gradeId);
    boolean existsByGrade_Id(UUID gradeId);
    List<GradeMonthlyFee> findAllByOrderByGrade_NameAsc();
}
