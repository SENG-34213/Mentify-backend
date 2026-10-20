package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.dto.response.StudentUpcomingExamResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.mapper.ExamMapper;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.StudentExamService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final ExamMapper examMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<StudentUpcomingExamResponse> getUpcomingExams() {
        UUID studentId = currentUserService.getCurrentUserId();
        LocalDate today = LocalDate.now();
        return examResultRepository.findUpcomingResultsWithExamByStudentId(studentId, today).stream()
                .map(row -> toUpcoming((Exam) row[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getStudentExam(UUID examId) {
        UUID studentId = currentUserService.getCurrentUserId();
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        if (exam.getStatus() != ExamStatus.SCHEDULED
                || !examResultRepository.existsByExamIdAndStudentId(examId, studentId)) {
            throw new ExamDomainException(HttpStatus.NOT_FOUND, "Exam is not available to this student");
        }
        return examMapper.toResponse(exam);
    }

    private StudentUpcomingExamResponse toUpcoming(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = exam.getExamDate().atTime(exam.getStartTime());
        LocalDateTime endsAt = exam.getExamDate().atTime(exam.getEndTime());
        boolean eligible = exam.getStatus() == ExamStatus.SCHEDULED
                && !now.isBefore(startsAt) && now.isBefore(endsAt);
        return StudentUpcomingExamResponse.builder()
                .examId(exam.getId()).courseId(exam.getCourseId()).title(exam.getTitle())
                .description(exam.getDescription()).examDate(exam.getExamDate())
                .startTime(exam.getStartTime()).endTime(exam.getEndTime())
                .totalMarks(exam.getTotalMarks()).passMarks(exam.getPassMarks())
                .type(exam.getType()).status(exam.getStatus()).eligible(eligible).build();
    }
}
