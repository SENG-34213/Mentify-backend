package com.mentify.exam.service.impl;

import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamResultService;
import com.mentify.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamResultServiceImpl implements ExamResultService {

    static final String ENROLLMENT_UNAVAILABLE_MESSAGE =
            "Enrollment Service is currently unavailable. Please try again later.";

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CourseAccessService courseAccessService;

    @Override
    @Transactional
    public List<ExamResult> initializeResults(UUID examId, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (exam.getStatus() == ExamStatus.CANCELLED) {
            throw new ExamDomainException(HttpStatus.CONFLICT, "Results cannot be initialized for a cancelled exam");
        }

        Set<UUID> eligibleStudents = fetchEligibleStudents(exam.getCourseId(), authorizationHeader);
        eligibleStudents.removeAll(examResultRepository.findStudentIdsByExamId(examId));
        if (eligibleStudents.isEmpty()) {
            return List.of();
        }

        List<ExamResult> newResults = eligibleStudents.stream()
                .map(studentId -> ExamResult.builder().examId(examId).studentId(studentId).build())
                .toList();
        return examResultRepository.saveAll(newResults);
    }

    private Set<UUID> fetchEligibleStudents(UUID courseId, String authorizationHeader) {
        try {
            var response = enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader);
            if (response == null || response.getData() == null) {
                return new LinkedHashSet<>();
            }
            Set<UUID> studentIds = new LinkedHashSet<>();
            response.getData().stream().filter(Objects::nonNull).forEach(studentIds::add);
            return studentIds;
        } catch (FeignException ex) {
            log.warn("Enrollment Service call failed with status {}", ex.status());
            throw new ExamDomainException(HttpStatus.SERVICE_UNAVAILABLE, ENROLLMENT_UNAVAILABLE_MESSAGE);
        }
    }
}
