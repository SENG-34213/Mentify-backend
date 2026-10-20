package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.mapper.ExamMapper;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.mentify.exam.exception.ExamDomainException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {

    @Mock private ExamResultRepository examResultRepository;
    @Mock private ExamRepository examRepository;
    @Mock private ExamMapper examMapper;
    @Mock private CurrentUserService currentUserService;

    private StudentExamServiceImpl service;
    private final UUID studentId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentExamServiceImpl(examResultRepository, examRepository, examMapper, currentUserService);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
    }

    @Test
    void returnsOnlyUpcomingExamsAssignedToTheAuthenticatedStudent() {
        Exam exam = Exam.builder().id(examId).courseId(UUID.randomUUID()).title("Midterm")
                .examDate(LocalDate.now().plusDays(1)).status(ExamStatus.SCHEDULED).build();
        when(examResultRepository.findUpcomingResultsWithExamByStudentId(studentId, LocalDate.now()))
                .thenReturn(List.of(new Object[]{new ExamResult(), exam}));

        var upcoming = service.getUpcomingExams();

        assertThat(upcoming).hasSize(1);
        assertThat(upcoming.get(0).getExamId()).isEqualTo(examId);
        assertThat(upcoming.get(0).isEligible()).isFalse();
    }

    @Test
    void refusesStudentDetailsWithoutAnAssignedScheduledResult() {
        Exam exam = Exam.builder().id(examId).status(ExamStatus.SCHEDULED).build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examResultRepository.existsByExamIdAndStudentId(examId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> service.getStudentExam(examId))
                .isInstanceOf(ExamDomainException.class);
    }

    @Test
    void returnsExamDetailsOnlyForAssignedStudents() {
        Exam exam = Exam.builder().id(examId).status(ExamStatus.SCHEDULED).build();
        ExamResponse expected = ExamResponse.builder().id(examId).build();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examResultRepository.existsByExamIdAndStudentId(examId, studentId)).thenReturn(true);
        when(examMapper.toResponse(exam)).thenReturn(expected);

        assertThat(service.getStudentExam(examId)).isEqualTo(expected);
    }
}
