package com.mentify.exam.service.impl;

import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ResultStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceImplTest {

    private static final String AUTH = "Bearer token";

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamResultRepository examResultRepository;
    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;
    @Mock
    private CourseAccessService courseAccessService;

    private ExamResultServiceImpl service;
    private final UUID examId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private Exam exam;

    @BeforeEach
    void setUp() {
        service = new ExamResultServiceImpl(
                examRepository, examResultRepository, enrollmentServiceClient, courseAccessService);
        exam = Exam.builder().id(examId).courseId(courseId).status(ExamStatus.SCHEDULED).build();
    }

    private void stubExam() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    }

    private void stubRoster(List<UUID> ids) {
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH))
                .thenReturn(ApiResponse.success(200, "ok", ids));
    }

    @Test
    void createsOneNotMarkedResultPerEligibleStudent() {
        List<UUID> students = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stubExam();
        stubRoster(students);
        when(examResultRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());
        when(examResultRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ExamResult> created = service.initializeResults(examId, AUTH);

        assertThat(created).hasSize(3);
        assertThat(created).extracting(ExamResult::getStudentId).containsExactlyInAnyOrderElementsOf(students);
        assertThat(created).allSatisfy(r -> {
            assertThat(r.getExamId()).isEqualTo(examId);
            assertThat(r.getResultStatus()).isEqualTo(ResultStatus.NOT_MARKED);
            assertThat(r.getMarksObtained()).isNull();
            assertThat(r.getAttendanceStatus()).isNull();
            assertThat(r.getMarkedBy()).isNull();
            assertThat(r.getMarkedAt()).isNull();
        });
        verify(courseAccessService).assertCanManage(courseId, AUTH);
    }

    @Test
    void skipsStudentsThatAlreadyHaveResultsAndIgnoresDuplicateRosterEntries() {
        UUID existing = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        stubExam();
        stubRoster(List.of(existing, fresh, fresh));
        when(examResultRepository.findStudentIdsByExamId(examId)).thenReturn(new HashSet<>(Set.of(existing)));
        when(examResultRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ExamResult> created = service.initializeResults(examId, AUTH);

        assertThat(created).extracting(ExamResult::getStudentId).containsExactly(fresh);
    }

    @Test
    void repeatedInitializationCreatesNothing() {
        UUID student = UUID.randomUUID();
        stubExam();
        stubRoster(List.of(student));
        when(examResultRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of(student));

        assertThat(service.initializeResults(examId, AUTH)).isEmpty();
        verify(examResultRepository, never()).saveAll(any());
    }

    @Test
    void emptyRosterCreatesNothing() {
        stubExam();
        stubRoster(List.of());
        when(examResultRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());

        assertThat(service.initializeResults(examId, AUTH)).isEmpty();
        verify(examResultRepository, never()).saveAll(any());
    }

    @Test
    void nullRosterBodyIsTreatedAsEmpty() {
        stubExam();
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH)).thenReturn(null);
        when(examResultRepository.findStudentIdsByExamId(examId)).thenReturn(Set.of());

        assertThat(service.initializeResults(examId, AUTH)).isEmpty();
    }

    @Test
    void enrollmentFailureFailsClosedWithServiceUnavailable() {
        stubExam();
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH))
                .thenThrow(new FeignException.InternalServerError("boom", request(), null, null));

        assertThatThrownBy(() -> service.initializeResults(examId, AUTH))
                .isInstanceOfSatisfying(ExamDomainException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(examResultRepository, never()).saveAll(any());
    }

    @Test
    void unknownExamIsNotFound() {
        when(examRepository.findById(examId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initializeResults(examId, AUTH))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unauthorizedCallerDoesNotReachEnrollmentService() {
        stubExam();
        doThrow(new AccessDeniedException("no")).when(courseAccessService).assertCanManage(courseId, AUTH);

        assertThatThrownBy(() -> service.initializeResults(examId, AUTH))
                .isInstanceOf(AccessDeniedException.class);
        verify(enrollmentServiceClient, never()).getEnrolledStudentIdsByCourse(any(), any());
    }

    @Test
    void cancelledExamCannotBeInitialized() {
        exam.setStatus(ExamStatus.CANCELLED);
        stubExam();

        assertThatThrownBy(() -> service.initializeResults(examId, AUTH))
                .isInstanceOfSatisfying(ExamDomainException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, null, null);
    }
}
