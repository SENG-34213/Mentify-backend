package com.mentify.exam.service.impl;

import com.mentify.exam.dto.response.ExamResultResponse;
import com.mentify.exam.dto.response.ExamResultSheetResponse;
import com.mentify.exam.client.UserServiceClient;
import com.mentify.exam.client.dto.StudentLookupRequest;
import com.mentify.exam.client.dto.StudentLookupResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        var results = examResultRepository.findByExamId(examId).stream()
                .sorted(Comparator.comparing(r -> r.getStudentId().toString()))
                .toList();
        Map<UUID, String> studentNames = lookupStudentNames(results, authorizationHeader);
        return ExamResultSheetResponse.builder()
                .examId(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .status(exam.getStatus())
                .totalMarks(exam.getTotalMarks())
                .passMarks(exam.getPassMarks())
                .results(results.stream().map(result -> toResponse(result, studentNames.get(result.getStudentId()))).toList())
                .build();
    }

    private Map<UUID, String> lookupStudentNames(java.util.List<ExamResult> results, String authorizationHeader) {
        if (results.isEmpty()) return Map.of();
        try {
            var response = userServiceClient.lookupStudents(
                    new StudentLookupRequest(results.stream().map(ExamResult::getStudentId).distinct().toList()),
                    authorizationHeader);
            if (response == null || response.getData() == null) return Map.of();
            return response.getData().stream().collect(Collectors.toMap(
                    StudentLookupResponse::getStudentId,
                    this::fullName,
                    (first, ignored) -> first,
                    HashMap::new));
        } catch (RuntimeException ignored) {
            // Results remain usable if the optional display-name lookup is temporarily unavailable.
            return Map.of();
        }
    }

    private String fullName(StudentLookupResponse student) {
        String name = Stream.of(student.getFirstName(), student.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return name.isBlank() ? null : name;
    }

    private ExamResultResponse toResponse(ExamResult r, String studentName) {
        return ExamResultResponse.builder()
                .studentId(r.getStudentId())
                .studentName(studentName)
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
