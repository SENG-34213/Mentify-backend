package com.mentify.exam.repository;

import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ExamRepositoryPersistenceTest {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsAllExamFieldsWithUuidAndAuditTimestamps() {
        UUID courseId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Exam exam = newExam(courseId, creatorId, LocalDate.of(2026, 10, 15), ExamStatus.SCHEDULED);

        Exam saved = examRepository.saveAndFlush(exam);
        entityManager.clear();

        Exam loaded = examRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getCourseId()).isEqualTo(courseId);
        assertThat(loaded.getTitle()).isEqualTo("Midterm examination");
        assertThat(loaded.getDescription()).isEqualTo("Bring identification to the examination.");
        assertThat(loaded.getExamDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(loaded.getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(loaded.getEndTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(loaded.getTotalMarks()).isEqualByComparingTo("100.00");
        assertThat(loaded.getPassMarks()).isEqualByComparingTo("40.00");
        assertThat(loaded.getType()).isEqualTo(ExamType.MIDTERM);
        assertThat(loaded.getStatus()).isEqualTo(ExamStatus.SCHEDULED);
        assertThat(loaded.getCreatedBy()).isEqualTo(creatorId);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isEqualTo(loaded.getCreatedAt());
    }

    @Test
    void persistsOptionalDescriptionAsNull() {
        Exam exam = newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT);
        exam.setDescription(null);

        Exam saved = examRepository.saveAndFlush(exam);
        entityManager.clear();

        assertThat(examRepository.findById(saved.getId()).orElseThrow().getDescription()).isNull();
    }

    @Test
    void persistsEverySupportedExamTypeAndLifecycleStatus() {
        UUID courseId = UUID.randomUUID();
        List<Exam> exams = new ArrayList<>();
        for (ExamType type : ExamType.values()) {
            for (ExamStatus status : ExamStatus.values()) {
                Exam exam = newExam(courseId, UUID.randomUUID(), LocalDate.of(2026, 10, 15), status);
                exam.setType(type);
                exams.add(exam);
            }
        }

        examRepository.saveAllAndFlush(exams);
        entityManager.clear();
        List<Exam> loaded = examRepository.findByCourseId(courseId);

        assertThat(loaded).hasSize(ExamType.values().length * ExamStatus.values().length);
        assertThat(loaded).extracting(Exam::getType).contains(ExamType.values());
        assertThat(loaded).extracting(Exam::getStatus).contains(ExamStatus.values());
    }

    @Test
    void updatesUpdatedAtWhenExamChanges() {
        Exam saved = examRepository.saveAndFlush(newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT));
        LocalDateTime createdAt = saved.getCreatedAt();

        saved.setUpdatedAt(createdAt.minusDays(1));
        saved.setTitle("Updated examination title");
        Exam updated = examRepository.saveAndFlush(saved);
        entityManager.clear();

        Exam loaded = examRepository.findById(updated.getId()).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Updated examination title");
        assertThat(loaded.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    void findsExamsByCourse() {
        UUID courseId = UUID.randomUUID();
        Exam first = examRepository.saveAndFlush(newExam(
                courseId, UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT));
        Exam second = examRepository.saveAndFlush(newExam(
                courseId, UUID.randomUUID(), LocalDate.of(2026, 11, 15), ExamStatus.SCHEDULED));
        examRepository.saveAndFlush(newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT));

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(examRepository.findByCourseId(courseId))
                .extracting(Exam::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void findsExamsByCourseAndStatus() {
        UUID courseId = UUID.randomUUID();
        Exam draft = examRepository.saveAndFlush(newExam(
                courseId, UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT));
        examRepository.saveAndFlush(newExam(
                courseId, UUID.randomUUID(), LocalDate.of(2026, 11, 15), ExamStatus.SCHEDULED));

        assertThat(examRepository.findByCourseIdAndStatus(courseId, ExamStatus.DRAFT))
                .extracting(Exam::getId)
                .containsExactly(draft.getId());
    }

    @Test
    void findsExamsWithinInclusiveDateRange() {
        Exam first = examRepository.saveAndFlush(newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 1), ExamStatus.DRAFT));
        Exam last = examRepository.saveAndFlush(newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 31), ExamStatus.COMPLETED));
        examRepository.saveAndFlush(newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 1), ExamStatus.DRAFT));

        List<Exam> exams = examRepository.findByExamDateBetween(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(exams).extracting(Exam::getId).containsExactlyInAnyOrder(first.getId(), last.getId());
    }

    @Test
    void rejectsPassMarksAboveTotalMarks() {
        Exam invalid = newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT);
        invalid.setPassMarks(new BigDecimal("100.01"));

        assertThatThrownBy(() -> examRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNonPositiveTotalMarks() {
        Exam invalid = newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT);
        invalid.setTotalMarks(BigDecimal.ZERO);

        assertThatThrownBy(() -> examRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        Exam invalid = newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT);
        invalid.setEndTime(LocalTime.of(9, 29));

        assertThatThrownBy(() -> examRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankTitles() {
        Exam invalid = newExam(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 15), ExamStatus.DRAFT);
        invalid.setTitle("   ");

        assertThatThrownBy(() -> examRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Exam newExam(UUID courseId, UUID createdBy, LocalDate examDate, ExamStatus status) {
        return Exam.builder()
                .courseId(courseId)
                .title("Midterm examination")
                .description("Bring identification to the examination.")
                .examDate(examDate)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(11, 30))
                .totalMarks(new BigDecimal("100.00"))
                .passMarks(new BigDecimal("40.00"))
                .type(ExamType.MIDTERM)
                .status(status)
                .createdBy(createdBy)
                .build();
    }
}
