package com.mentify.service.impl;

import com.mentify.client.CourseServiceClient;
import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.entity.Entrollment;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.EntrollmentRepository;
import com.mentify.service.EntrollmentService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntrollmentServiceImpl implements EntrollmentService {

    private final EntrollmentRepository entrollmentRepository;
    private final CourseServiceClient courseServiceClient;

    @Override
    @Transactional
    public ApiResponse<EntrollmentResponse> createEntrollment(EntrollmentCreateRequest request, String authorizationHeader) {
        log.info("Creating enrollment for student [{}]", request.getStudentId());

        Set<UUID> requestedCourseIds = sanitizeAndValidateCourseIds(request.getCourseIds());
        validateCoursesExist(requestedCourseIds, authorizationHeader);
        validateNoDuplicateEnrollment(request.getStudentId(), requestedCourseIds);

        Entrollment entrollment = Entrollment.builder()
                .studentId(request.getStudentId())
                .courseIds(requestedCourseIds)
                .enrolledOn(LocalDate.now())
                .build();

        Entrollment savedEntrollment = entrollmentRepository.save(entrollment);

        return ApiResponse.<EntrollmentResponse>builder()
                .message("Enrollment created successfully")
                .data(toResponse(savedEntrollment))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    private Set<UUID> sanitizeAndValidateCourseIds(List<UUID> courseIds) {
        Set<UUID> sanitized = courseIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("At least one valid course ID is required");
        }

        return sanitized;
    }

    private void validateCoursesExist(Set<UUID> courseIds, String authorizationHeader) {
        for (UUID courseId : courseIds) {
            try {
                courseServiceClient.getCourseById(courseId, authorizationHeader);
            } catch (FeignException.NotFound ex) {
                throw new ResourceNotFoundException("Course", "id", courseId);
            } catch (FeignException ex) {
                throw new IllegalStateException("Failed to validate course with ID: " + courseId, ex);
            }
        }
    }

    private void validateNoDuplicateEnrollment(UUID studentId, Set<UUID> requestedCourseIds) {
        List<Entrollment> existingEntrollments = entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId);

        Set<UUID> alreadyEnrolledCourseIds = existingEntrollments.stream()
                .flatMap(entrollment -> entrollment.getCourseIds().stream())
                .collect(Collectors.toSet());

        Set<UUID> duplicates = requestedCourseIds.stream()
                .filter(alreadyEnrolledCourseIds::contains)
                .collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            throw new ResourceAlreadyExistsException(
                    "Enrollment already exists for student " + studentId + " in courses: " + duplicates
            );
        }
    }

    private EntrollmentResponse toResponse(Entrollment entrollment) {
        return EntrollmentResponse.builder()
                .id(entrollment.getId())
                .studentId(entrollment.getStudentId())
                .courseIds(entrollment.getCourseIds())
                .enrolledOn(entrollment.getEnrolledOn())
                .createdBy(entrollment.getCreatedBy())
                .lastModifiedBy(entrollment.getUpdatedBy())
                .createdAt(entrollment.getCreatedAt())
                .updatedAt(entrollment.getUpdatedAt())
                .build();
    }
}

