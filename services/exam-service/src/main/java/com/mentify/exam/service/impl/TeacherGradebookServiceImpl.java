package com.mentify.exam.service.impl;

import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.client.UserServiceClient;
import com.mentify.exam.client.dto.StudentLookupResponse;
import com.mentify.exam.dto.response.TeacherGradebookResponse;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.TeacherGradebookService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherGradebookServiceImpl implements TeacherGradebookService {

    private final CourseAccessService courseAccessService;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final UserServiceClient userServiceClient;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherGradebookResponse getCourseGradebook(UUID courseId, String authorizationHeader) {
        courseAccessService.assertCanManage(courseId, authorizationHeader);
        var rosterResponse = enrollmentServiceClient
                .getEnrolledStudentIdsByCourse(courseId, authorizationHeader);
        List<UUID> studentIds = rosterResponse == null ? null : rosterResponse.getData();
        if (studentIds == null) {
            throw new ResourceNotFoundException("Student roster", "courseId", courseId);
        }

        var exams = examRepository.findByCourseId(courseId).stream()
                .filter(exam -> exam.getStatus() == ExamStatus.COMPLETED)
                .map(exam -> TeacherGradebookResponse.GradebookExamResponse.builder()
                        .examId(exam.getId())
                        .title(exam.getTitle())
                        .examDate(exam.getExamDate())
                        .status(exam.getStatus())
                        .totalMarks(exam.getTotalMarks())
                        .passMarks(exam.getPassMarks())
                        .build())
                .toList();
        Map<UUID, StudentLookupResponse> students = lookupStudents(studentIds, authorizationHeader);
        Map<UUID, Map<UUID, ExamResult>> resultsByStudent = examRepository.findByCourseId(courseId).stream()
                .filter(exam -> exam.getStatus() == ExamStatus.COMPLETED)
                .flatMap(exam -> examResultRepository.findByExamId(exam.getId()).stream())
                .collect(Collectors.groupingBy(ExamResult::getStudentId,
                        Collectors.toMap(ExamResult::getExamId, Function.identity(), (first, ignored) -> first)));

        var gradebookStudents = studentIds.stream().distinct().map(studentId -> {
            StudentLookupResponse student = students.get(studentId);
            return TeacherGradebookResponse.GradebookStudentResponse.builder()
                    .studentId(studentId)
                    .studentName(student == null ? null : (student.getFirstName() + " " + student.getLastName()).trim())
                    .studentNumber(student == null ? null : student.getStudentNumber())
                    .email(student == null ? null : student.getEmail())
                    .results(exams.stream()
                            .map(exam -> toMark(exam.getExamId(), resultsByStudent.getOrDefault(studentId, Map.of()).get(exam.getExamId())))
                            .toList())
                    .build();
        }).toList();

        return TeacherGradebookResponse.builder()
                .courseId(courseId)
                .exams(exams)
                .students(gradebookStudents)
                .build();
    }

    private Map<UUID, StudentLookupResponse> lookupStudents(List<UUID> ids, String authorizationHeader) {
        var response = userServiceClient.lookupStudents(ids, authorizationHeader);
        return response == null || response.getData() == null ? Map.of() : response.getData().stream()
                .collect(Collectors.toMap(StudentLookupResponse::getId, Function.identity(), (first, ignored) -> first));
    }

    private TeacherGradebookResponse.GradebookMarkResponse toMark(UUID examId, ExamResult result) {
        return TeacherGradebookResponse.GradebookMarkResponse.builder()
                .examId(examId)
                .marksObtained(result == null ? null : result.getMarksObtained())
                .attendanceStatus(result == null || result.getAttendanceStatus() == null ? null : result.getAttendanceStatus().name())
                .resultStatus(result == null || result.getResultStatus() == null ? null : result.getResultStatus().name())
                .grade(result == null ? null : result.getGrade())
                .remarks(result == null ? null : result.getRemarks())
                .build();
    }
}
