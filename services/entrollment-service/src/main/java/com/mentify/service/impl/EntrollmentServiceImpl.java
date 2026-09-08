package com.mentify.service.impl;

import com.mentify.client.CommunicationServiceClient;
import com.mentify.client.CourseServiceClient;
import com.mentify.client.dto.AddStudentToGroupRequest;
import com.mentify.client.dto.CourseBulkLookupRequest;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.dto.EntrollmentUpdateRequest;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntrollmentServiceImpl implements EntrollmentService {

    private final EntrollmentRepository entrollmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final CommunicationServiceClient communicationServiceClient;

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
        syncStudentToCommunicationGroups(savedEntrollment.getStudentId(), savedEntrollment.getCourseIds(), authorizationHeader);

        return ApiResponse.<EntrollmentResponse>builder()
                .message("Enrollment created successfully")
                .data(toResponse(savedEntrollment))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<EntrollmentResponse> updateEntrollment(
            UUID enrollmentId,
            EntrollmentUpdateRequest request,
            String authorizationHeader
    ) {
        log.info("Updating enrollment [{}]", enrollmentId);

        Entrollment existingEnrollment = entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        Set<UUID> existingCourseIds = new HashSet<>(existingEnrollment.getCourseIds());
        Set<UUID> requestedCourseIds = sanitizeAndValidateCourseIds(request.getCourseIds());
        validateCoursesExist(requestedCourseIds, authorizationHeader);

        existingEnrollment.setCourseIds(requestedCourseIds);
        Entrollment savedEnrollment = entrollmentRepository.save(existingEnrollment);
        Set<UUID> newlyAddedCourseIds = requestedCourseIds.stream()
                .filter(courseId -> !existingCourseIds.contains(courseId))
                .collect(Collectors.toSet());
        syncStudentToCommunicationGroups(savedEnrollment.getStudentId(), newlyAddedCourseIds, authorizationHeader);

        return ApiResponse.<EntrollmentResponse>builder()
                .message("Enrollment updated successfully")
                .data(toResponse(savedEnrollment))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public boolean isStudentEnrolledInCourse(UUID studentId, UUID courseId) {
        return entrollmentRepository.existsActiveEnrollmentForStudentAndCourse(studentId, courseId);
    }

    @Override
    public ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(UUID courseId) {
        List<UUID> studentIds = entrollmentRepository.findActiveStudentIdsByCourseId(courseId);

        return ApiResponse.<List<UUID>>builder()
                .message("Enrolled students fetched successfully")
                .data(studentIds)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private Set<UUID> sanitizeAndValidateCourseIds(List<UUID> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("At least one course ID is required");
        }

        if (courseIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Course IDs cannot contain null values");
        }

        Set<UUID> sanitized = new HashSet<>(courseIds);

        if (sanitized.size() != courseIds.size()) {
            throw new IllegalArgumentException("Duplicate course IDs are not allowed");
        }

        return sanitized;
    }

    private void validateCoursesExist(Set<UUID> courseIds, String authorizationHeader) {
        try {
            ApiResponse<List<CourseLookupResponse>> response = courseServiceClient.getCoursesByIds(
                    CourseBulkLookupRequest.builder().ids(courseIds).build(),
                    authorizationHeader
            );

            List<CourseLookupResponse> courses = response.getData() != null ? response.getData() : List.of();

            Set<UUID> foundCourseIds = courses.stream()
                    .map(CourseLookupResponse::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<UUID> missingCourseIds = courseIds.stream()
                    .filter(id -> !foundCourseIds.contains(id))
                    .collect(Collectors.toSet());

            if (!missingCourseIds.isEmpty()) {
                throw new ResourceNotFoundException("Course", "id", missingCourseIds.iterator().next());
            }
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "ids", courseIds);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate courses", ex);
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

    private void syncStudentToCommunicationGroups(UUID studentId, Set<UUID> courseIds, String authorizationHeader) {
        for (UUID courseId : courseIds) {
            try {
                communicationServiceClient.addStudentToCourseGroup(
                        courseId,
                        AddStudentToGroupRequest.builder()
                                .studentId(studentId)
                                .build(),
                        authorizationHeader
                );
            } catch (FeignException.NotFound ex) {
                log.info("No active communication group found for course [{}]; student [{}] will be synced when the group is created", courseId, studentId);
            } catch (FeignException ex) {
                throw new IllegalStateException("Failed to add student to communication group for course " + courseId, ex);
            }
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
