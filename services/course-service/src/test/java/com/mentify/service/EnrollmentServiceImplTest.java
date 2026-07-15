package com.mentify.service;

import com.mentify.dto.EnrollmentRequest;
import com.mentify.dto.EnrollmentResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Enrollment;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.EnrollmentRepository;
import com.mentify.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentValidationService studentValidationService;

    private EnrollmentServiceImpl enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentServiceImpl(enrollmentRepository, courseRepository, studentValidationService);
    }

    @Test
    void createEnrollment_whenRequestIsValid_returnsCreatedResponse() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId(studentId)
                .courseId(courseId)
                .build();

        Course course = new Course();
        course.setId(courseId);

        when(studentValidationService.studentExists(studentId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourse_Id(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(UUID.randomUUID());
            return enrollment;
        });

        ApiResponse<EnrollmentResponse> response = enrollmentService.createEnrollment(request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Enrollment created successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getStudentId()).isEqualTo(studentId);
        assertThat(response.getData().getCourseId()).isEqualTo(courseId);
    }

    @Test
    void createEnrollment_whenStudentDoesNotExist_throwsResourceNotFoundException() {
        UUID studentId = UUID.randomUUID();
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId(studentId)
                .courseId(UUID.randomUUID())
                .build();

        when(studentValidationService.studentExists(studentId)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student not found with id: '" + studentId + "'");

        verify(courseRepository, never()).findById(any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createEnrollment_whenCourseDoesNotExist_throwsResourceNotFoundException() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId(studentId)
                .courseId(courseId)
                .build();

        when(studentValidationService.studentExists(studentId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found with id: '" + courseId + "'");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createEnrollment_whenDuplicateEnrollmentExists_throwsResourceAlreadyExistsException() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId(studentId)
                .courseId(courseId)
                .build();

        Course course = new Course();
        course.setId(courseId);

        when(studentValidationService.studentExists(studentId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourse_Id(studentId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Enrollment already exists for the provided student and course");

        verify(enrollmentRepository, never()).save(any());
    }
}
