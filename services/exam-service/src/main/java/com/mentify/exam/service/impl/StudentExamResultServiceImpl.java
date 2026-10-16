package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.StudentExamResultResponse;
import com.mentify.exam.dto.response.StudentExamResultsResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.StudentExamResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentExamResultServiceImpl implements StudentExamResultService {

    private final ExamResultRepository examResultRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public StudentExamResultsResponse getMyResults() {
        var studentId = currentUserService.getCurrentUserId();
        return new StudentExamResultsResponse(
                examResultRepository.findCompletedResultsWithExamByStudentId(studentId).stream()
                        .map(row -> toResponse((ExamResult) row[0], (Exam) row[1]))
                        .toList());
    }

    private StudentExamResultResponse toResponse(ExamResult r, Exam e) {
        return StudentExamResultResponse.builder()
                .examId(e.getId())
                .courseId(e.getCourseId())
                .title(e.getTitle())
                .examDate(e.getExamDate())
                .type(e.getType())
                .examStatus(e.getStatus())
                .totalMarks(e.getTotalMarks())
                .passMarks(e.getPassMarks())
                .attendanceStatus(r.getAttendanceStatus())
                .marksObtained(r.getMarksObtained())
                .resultStatus(r.getResultStatus())
                .grade(r.getGrade())
                .remarks(r.getRemarks())
                .build();
    }
}