package com.mentify.exam.repository;

import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findByCourseId(UUID courseId);

    List<Exam> findByCourseIdAndStatus(UUID courseId, ExamStatus status);

    List<Exam> findByExamDateBetween(LocalDate startDate, LocalDate endDate);
}
