package com.mentify.service.impl;

import com.mentify.dto.EnrollmentRequest;
import com.mentify.dto.EnrollmentResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Enrollment;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.EnrollmentRepository;
import com.mentify.service.EnrollmentService;
import com.mentify.service.StudentValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentValidationService studentValidationService;

    @Override
    @Transactional
    public ApiResponse<EnrollmentResponse> createEnrollment(EnrollmentRequest request) {
        log.info("Creating enrollment for student [{}] and course [{}]", request.getStudentId(), request.getCourseId());

        if (!studentValidationService.studentExists(request.getStudentId())) {
            throw new ResourceNotFoundException("Student", "id", request.getStudentId());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        if (enrollmentRepository.existsByStudentIdAndCourse_Id(request.getStudentId(), request.getCourseId())) {
            throw new ResourceAlreadyExistsException("Enrollment already exists for the provided student and course");
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .course(course)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(savedEnrollment.getId())
                .studentId(savedEnrollment.getStudentId())
                .courseId(savedEnrollment.getCourse().getId())
                .build();

        return ApiResponse.<EnrollmentResponse>builder()
                .message("Enrollment created successfully")
                .data(response)
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }
}
