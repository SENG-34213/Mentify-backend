package com.mentify.exam.repository;

import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {

    List<ExamResult> findByExamId(UUID examId);

    List<ExamResult> findByStudentId(UUID studentId);

    Optional<ExamResult> findByExamIdAndStudentId(UUID examId, UUID studentId);

    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);

    @Query("select r.studentId from ExamResult r where r.examId = :examId")
    Set<UUID> findStudentIdsByExamId(UUID examId);

    @Query("select r, e from ExamResult r, Exam e where e.id = r.examId and r.studentId = :studentId "
            + "and e.status = com.mentify.exam.enums.ExamStatus.COMPLETED order by e.examDate desc, e.title asc")
    List<Object[]> findCompletedResultsWithExamByStudentId(UUID studentId);

    @Query("select r, e from ExamResult r, Exam e where e.id = r.examId and r.studentId = :studentId "
            + "and e.status = com.mentify.exam.enums.ExamStatus.SCHEDULED and e.examDate >= :today "
            + "order by e.examDate asc, e.startTime asc")
    List<Object[]> findUpcomingResultsWithExamByStudentId(UUID studentId, LocalDate today);
}
