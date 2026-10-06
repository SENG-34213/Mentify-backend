package com.mentify.exam.repository;

import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static com.mentify.exam.enums.ResultStatus.NOT_MARKED;
import static com.mentify.exam.enums.ResultStatus.PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pinned so CI's SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop cannot replace the Flyway schema under test.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
class ExamResultRepositoryPersistenceTest {

    @Autowired
    private ExamResultRepository repository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsInitialStateWithNullMarksAndMarkerFields() {
        Exam exam = saveExam();
        UUID studentId = UUID.randomUUID();

        ExamResult saved = repository.saveAndFlush(
                ExamResult.builder().examId(exam.getId()).studentId(studentId).build());
        entityManager.clear();

        ExamResult loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getExamId()).isEqualTo(exam.getId());
        assertThat(loaded.getStudentId()).isEqualTo(studentId);
        assertThat(loaded.getResultStatus()).isEqualTo(NOT_MARKED);
        assertThat(loaded.getMarksObtained()).isNull();
        assertThat(loaded.getAttendanceStatus()).isNull();
        assertThat(loaded.getGrade()).isNull();
        assertThat(loaded.getMarkedBy()).isNull();
        assertThat(loaded.getMarkedAt()).isNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void persistsMarkedResultValues() {
        Exam exam = saveExam();
        ExamResult result = ExamResult.builder().examId(exam.getId()).studentId(UUID.randomUUID())
                .marksObtained(new BigDecimal("55.50")).attendanceStatus(AttendanceStatus.PRESENT)
                .resultStatus(PASS).build();

        ExamResult saved = repository.saveAndFlush(result);
        entityManager.clear();

        ExamResult loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getMarksObtained()).isEqualByComparingTo("55.50");
        assertThat(loaded.getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(loaded.getResultStatus()).isEqualTo(PASS);
    }

    @Test
    void rejectsDuplicateResultForSameExamAndStudent() {
        Exam exam = saveExam();
        UUID studentId = UUID.randomUUID();
        repository.saveAndFlush(ExamResult.builder().examId(exam.getId()).studentId(studentId).build());

        assertThatThrownBy(() -> repository.saveAndFlush(
                ExamResult.builder().examId(exam.getId()).studentId(studentId).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameStudentAcrossDifferentExams() {
        Exam first = saveExam();
        Exam second = saveExam();
        UUID studentId = UUID.randomUUID();

        repository.saveAndFlush(ExamResult.builder().examId(first.getId()).studentId(studentId).build());
        repository.saveAndFlush(ExamResult.builder().examId(second.getId()).studentId(studentId).build());

        assertThat(repository.findByStudentId(studentId)).hasSize(2);
    }

    @Test
    void rejectsResultForUnknownExam() {
        assertThatThrownBy(() -> repository.saveAndFlush(
                ExamResult.builder().examId(UUID.randomUUID()).studentId(UUID.randomUUID()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeMarks() {
        Exam exam = saveExam();
        ExamResult result = ExamResult.builder().examId(exam.getId()).studentId(UUID.randomUUID())
                .marksObtained(new BigDecimal("-1.00")).build();

        assertThatThrownBy(() -> repository.saveAndFlush(result))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void queriesByExamStudentAndBoth() {
        Exam exam = saveExam();
        Exam other = saveExam();
        UUID studentA = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        repository.save(ExamResult.builder().examId(exam.getId()).studentId(studentA).build());
        repository.save(ExamResult.builder().examId(exam.getId()).studentId(studentB).build());
        repository.saveAndFlush(ExamResult.builder().examId(other.getId()).studentId(studentA).build());

        assertThat(repository.findByExamId(exam.getId())).hasSize(2);
        assertThat(repository.findByStudentId(studentA)).hasSize(2);
        assertThat(repository.findByExamIdAndStudentId(exam.getId(), studentB)).isPresent();
        assertThat(repository.findByExamIdAndStudentId(other.getId(), studentB)).isEmpty();
        assertThat(repository.existsByExamIdAndStudentId(exam.getId(), studentA)).isTrue();
        assertThat(repository.existsByExamIdAndStudentId(other.getId(), studentB)).isFalse();
        assertThat(repository.findStudentIdsByExamId(exam.getId())).containsExactlyInAnyOrder(studentA, studentB);
    }

    private Exam saveExam() {
        return examRepository.saveAndFlush(Exam.builder()
                .courseId(UUID.randomUUID()).title("Midterm").examDate(LocalDate.of(2026, 10, 15))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .totalMarks(new BigDecimal("100")).passMarks(new BigDecimal("40"))
                .type(ExamType.MIDTERM).status(ExamStatus.SCHEDULED).createdBy(UUID.randomUUID()).build());
    }
}
