package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamResultResponse;
import com.mentify.exam.dto.response.ExamResultSheetResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.client.UserServiceClient;
import com.mentify.exam.client.dto.StudentLookupResponse;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamResultSheetService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamResultSheetServiceImpl implements ExamResultSheetService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseAccessService courseAccessService;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional(readOnly = true)
    public ExamResultSheetResponse getResultSheet(UUID examId, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        var examResults = examResultRepository.findByExamId(examId).stream()
                .sorted(Comparator.comparing(r -> r.getStudentId().toString()))
                .toList();
        Map<UUID, StudentLookupResponse> students = lookupStudents(examResults, authorizationHeader);
        return ExamResultSheetResponse.builder()
                .examId(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .status(exam.getStatus())
                .totalMarks(exam.getTotalMarks())
                .passMarks(exam.getPassMarks())
                .results(examResults.stream()
                        .map(result -> toResponse(result, students.get(result.getStudentId())))
                        .toList())
                .build();
    }

    private Map<UUID, StudentLookupResponse> lookupStudents(
            java.util.List<ExamResult> results, String authorizationHeader) {
        var ids = results.stream().map(ExamResult::getStudentId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var response = userServiceClient.lookupStudents(ids, authorizationHeader);
        return response == null || response.getData() == null
                ? Map.of()
                : response.getData().stream().collect(Collectors.toMap(
                        StudentLookupResponse::getId, Function.identity(), (first, ignored) -> first));
    }

    private ExamResultResponse toResponse(ExamResult r, StudentLookupResponse student) {
        return ExamResultResponse.builder()
                .studentId(r.getStudentId())
                .studentName(student == null ? null : (student.getFirstName() + " " + student.getLastName()).trim())
                .studentNumber(student == null ? null : student.getStudentNumber())
                .studentEmail(student == null ? null : student.getEmail())
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
