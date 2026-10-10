package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamSummaryResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ResultStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamSummaryService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamSummaryServiceImpl implements ExamSummaryService {

    static final int AVERAGE_SCALE = 2;

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseAccessService courseAccessService;

    @Override
    @Transactional(readOnly = true)
    public ExamSummaryResponse getSummary(UUID examId, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (exam.getStatus() == ExamStatus.CANCELLED) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Summary is not available for a cancelled exam");
        }

        List<ExamResult> results = examResultRepository.findByExamId(examId);
        long present = results.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatus.PRESENT).count();
        long absent = results.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatus.ABSENT).count();
        long passed = results.stream().filter(r -> r.getResultStatus() == ResultStatus.PASS).count();
        long failed = results.stream().filter(r -> r.getResultStatus() == ResultStatus.FAIL).count();

        List<BigDecimal> marks = results.stream()
                .filter(r -> r.getAttendanceStatus() == AttendanceStatus.PRESENT && r.getMarksObtained() != null)
                .map(ExamResult::getMarksObtained)
                .toList();

        BigDecimal average = null;
        BigDecimal highest = null;
        BigDecimal lowest = null;
        if (!marks.isEmpty()) {
            average = marks.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(marks.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
            highest = marks.stream().max(BigDecimal::compareTo).orElse(null);
            lowest = marks.stream().min(BigDecimal::compareTo).orElse(null);
        }

        return ExamSummaryResponse.builder()
                .examId(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .status(exam.getStatus())
                .totalMarks(exam.getTotalMarks())
                .passMarks(exam.getPassMarks())
                .totalStudents(results.size())
                .presentStudents(present)
                .absentStudents(absent)
                .passedStudents(passed)
                .failedStudents(failed)
                .averageMarks(average)
                .highestMarks(highest)
                .lowestMarks(lowest)
                .build();
    }
}