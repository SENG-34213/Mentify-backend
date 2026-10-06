package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamResultResponse;
import com.mentify.exam.dto.response.ExamResultSheetResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamResultSheetService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamResultSheetServiceImpl implements ExamResultSheetService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseAccessService courseAccessService;

    @Override
    @Transactional(readOnly = true)
    public ExamResultSheetResponse getResultSheet(UUID examId, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        return ExamResultSheetResponse.builder()
                .examId(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .status(exam.getStatus())
                .totalMarks(exam.getTotalMarks())
                .passMarks(exam.getPassMarks())
                .results(examResultRepository.findByExamId(examId).stream()
                        .sorted(Comparator.comparing(r -> r.getStudentId().toString()))
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    private ExamResultResponse toResponse(ExamResult r) {
        return ExamResultResponse.builder()
                .studentId(r.getStudentId())
                .attendanceStatus(r.getAttendanceStatus())
                .marksObtained(r.getMarksObtained())
                .resultStatus(r.getResultStatus())
                .grade(r.getGrade())
                .remarks(r.getRemarks())
                .markedBy(r.getMarkedBy())
                .markedAt(r.getMarkedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}