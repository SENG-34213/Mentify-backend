package com.mentify.service.impl;

import com.mentify.client.CommunicationServiceClient;
import com.mentify.client.CourseServiceClient;
import com.mentify.client.PaymentServiceClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.AddStudentToGroupRequest;
import com.mentify.client.dto.CourseBulkLookupRequest;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.client.dto.PaymentVerificationResponse;
import com.mentify.client.dto.UserLookupResponse;
import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.dto.EntrollmentUpdateRequest;
import com.mentify.dto.CoursePopularityResponse;
import com.mentify.dto.EnrollmentAnalyticsOverviewResponse;
import com.mentify.dto.StudentEntrollmentCreateRequest;
import com.mentify.dto.UnenrolledStudentResponse;
import com.mentify.entity.Entrollment;
import com.mentify.enums.EnrollmentStatus;
import com.mentify.exception.PaymentRequiredException;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.EntrollmentRepository;
import com.mentify.security.CurrentUserService;
import com.mentify.service.EntrollmentService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final PaymentServiceClient paymentServiceClient;
    private final CommunicationServiceClient communicationServiceClient;
    private final UserServiceClient userServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<EntrollmentResponse> createEntrollment(EntrollmentCreateRequest request, String authorizationHeader) {
        log.info("Creating enrollment for student [{}]", request.getStudentId());

        Set<UUID> requestedCourseIds = sanitizeAndValidateCourseIds(request.getCourseIds());
        List<CourseLookupResponse> courses = validateCoursesExist(requestedCourseIds, authorizationHeader);
        assertTeacherCanManageCourses(courses);
        validateNoDuplicateEnrollment(request.getStudentId(), requestedCourseIds);

        EnrollmentStatus status = request.getStatus() != null
                ? request.getStatus()
                : EnrollmentStatus.APPROVED;
        return saveEnrollment(request.getStudentId(), requestedCourseIds, authorizationHeader, true, status);
    }

    @Override
    @Transactional
    public ApiResponse<EntrollmentResponse> createCurrentStudentEntrollment(
            StudentEntrollmentCreateRequest request,
            String authorizationHeader
    ) {
        UUID studentId = currentUserService.getCurrentUserId();
        log.info("Creating self-service enrollment for student [{}]", studentId);

        Set<UUID> requestedCourseIds = sanitizeAndValidateCourseIds(request.getCourseIds());
        List<CourseLookupResponse> courses = lookupCoursesForStudent(requestedCourseIds, authorizationHeader);
        validateNoDuplicateEnrollment(studentId, requestedCourseIds);
        validateSuccessfulPaymentsForPaidCourses(studentId, courses, requestedCourseIds, authorizationHeader);

        return saveEnrollment(studentId, requestedCourseIds, authorizationHeader, false, EnrollmentStatus.PENDING);
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
        if (isTeacherAuthenticated()) {
            assertTeacherCanManageCourses(validateCoursesExist(existingCourseIds, authorizationHeader));
        }
        boolean courseIdsProvided = request.getCourseIds() != null && !request.getCourseIds().isEmpty();
        if (!courseIdsProvided && request.getStatus() == null) {
            throw new IllegalArgumentException("Course IDs or enrollment status is required");
        }
        Set<UUID> requestedCourseIds = courseIdsProvided
                ? sanitizeAndValidateCourseIds(request.getCourseIds())
                : existingCourseIds;
        List<CourseLookupResponse> courses = courseIdsProvided
                ? validateCoursesExist(requestedCourseIds, authorizationHeader)
                : List.of();
        assertTeacherCanManageCourses(courses);
        Set<UUID> newlyAddedCourseIds = requestedCourseIds.stream()
                .filter(courseId -> !existingCourseIds.contains(courseId))
                .collect(Collectors.toSet());
        existingEnrollment.setCourseIds(requestedCourseIds);
        if (request.getStatus() != null) {
            existingEnrollment.setStatus(request.getStatus());
        }
        Entrollment savedEnrollment = entrollmentRepository.save(existingEnrollment);
        syncStudentToCommunicationGroups(savedEnrollment.getStudentId(), newlyAddedCourseIds, authorizationHeader, true);

        return ApiResponse.<EntrollmentResponse>builder()
                .message("Enrollment updated successfully")
                .data(toResponse(savedEnrollment))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteEntrollment(UUID enrollmentId, String authorizationHeader) {
        Entrollment existingEnrollment = entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        if (isTeacherAuthenticated()) {
            assertTeacherCanManageCourses(
                    validateCoursesExist(existingEnrollment.getCourseIds(), authorizationHeader));
        }

        existingEnrollment.setActive(false);
        entrollmentRepository.save(existingEnrollment);

        return ApiResponse.<Void>builder()
                .message("Enrollment deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public boolean isStudentEnrolledInCourse(UUID studentId, UUID courseId) {
        return entrollmentRepository.existsActiveEnrollmentForStudentAndCourse(studentId, courseId);
    }

    @Override
    public ApiResponse<List<UUID>> getActiveCourseIdsForStudent(UUID studentId) {
        List<UUID> courseIds = entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId).stream()
                .flatMap(enrollment -> enrollment.getCourseIds().stream())
                .distinct()
                .toList();
        return ApiResponse.<List<UUID>>builder()
                .message("Student course scope fetched successfully")
                .data(courseIds)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(UUID courseId, String authorizationHeader) {
        if (isTeacherAuthenticated()) {
            assertTeacherCanManageCourses(List.of(lookupCourseById(courseId, authorizationHeader)));
        }
        List<UUID> studentIds = entrollmentRepository.findActiveStudentIdsByCourseId(courseId);

        return ApiResponse.<List<UUID>>builder()
                .message("Enrolled students fetched successfully")
                .data(studentIds)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse<List<EntrollmentResponse>> getActiveEntrollmentsByCourse(
            UUID courseId,
            String authorizationHeader
    ) {
        CourseLookupResponse course = lookupCourseById(courseId, authorizationHeader);
        assertTeacherCanManageCourses(List.of(course));

        List<EntrollmentResponse> enrollments = entrollmentRepository
                .findAllByCourseIdAndIsActiveTrue(courseId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<List<EntrollmentResponse>>builder()
                .message("Course enrollments fetched successfully")
                .data(enrollments)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<EntrollmentResponse>> getActiveEntrollments() {
        List<EntrollmentResponse> enrollments = entrollmentRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<List<EntrollmentResponse>>builder()
                .message("Active enrollments fetched successfully")
                .data(enrollments)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public ApiResponse<List<UnenrolledStudentResponse>> getUnenrolledStudents(String authorizationHeader) {
        Set<UUID> enrolledStudentIds = new HashSet<>(entrollmentRepository.findActiveStudentIds());
        List<UserLookupResponse> students = getStudents(authorizationHeader);

        List<UnenrolledStudentResponse> unenrolledStudents = students.stream()
                .filter(student -> student.getId() != null)
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .map(UnenrolledStudentResponse::from)
                .toList();

        return ApiResponse.<List<UnenrolledStudentResponse>>builder()
                .message("Unenrolled students fetched successfully")
                .data(unenrolledStudents)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<EnrollmentAnalyticsOverviewResponse> getAnalyticsOverview() {
        return analyticsResponse(entrollmentRepository.findAllByIsActiveTrueOrderByCreatedAtDesc(), null);
    }

    @Override
    @Transactional
    public ApiResponse<EnrollmentAnalyticsOverviewResponse> getAnalyticsCourseSummary(UUID courseId) {
        return analyticsResponse(entrollmentRepository.findAllByCourseIdAndIsActiveTrue(courseId), courseId);
    }

    private ApiResponse<EnrollmentAnalyticsOverviewResponse> analyticsResponse(
            List<Entrollment> source,
            UUID courseId
    ) {
        List<Entrollment> approved = source.stream()
                .filter(this::isAnalyticsEnrollment)
                .toList();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, Long> trend = approved.stream()
                .filter(enrollment -> enrollment.getEnrolledOn() != null)
                .collect(Collectors.groupingBy(
                        enrollment -> YearMonth.from(enrollment.getEnrolledOn()).format(monthFormatter),
                        java.util.TreeMap::new,
                        Collectors.counting()
                ));
        List<CoursePopularityResponse> popularity = approved.stream()
                .flatMap(enrollment -> enrollment.getCourseIds().stream())
                .filter(course -> courseId == null || courseId.equals(course))
                .collect(Collectors.groupingBy(course -> course, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new CoursePopularityResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(CoursePopularityResponse::enrollmentCount).reversed()
                        .thenComparing(result -> result.courseId().toString()))
                .toList();
        EnrollmentAnalyticsOverviewResponse data = new EnrollmentAnalyticsOverviewResponse(
                approved.size(),
                approved.stream().map(Entrollment::getStudentId).distinct().count(),
                approved.stream().filter(enrollment -> enrollment.getEnrolledOn() != null
                        && !enrollment.getEnrolledOn().isBefore(monthStart)).count(),
                popularity,
                trend
        );
        return ApiResponse.<EnrollmentAnalyticsOverviewResponse>builder()
                .message("Enrollment analytics fetched successfully")
                .data(data)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private boolean isAnalyticsEnrollment(Entrollment enrollment) {
        return enrollment.getStatus() == null || enrollment.getStatus() == EnrollmentStatus.APPROVED;
    }

    private List<UserLookupResponse> getStudents(String authorizationHeader) {
        try {
            ApiResponse<List<UserLookupResponse>> response = userServiceClient.getUsersByRole("STUDENT", authorizationHeader);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to fetch students", ex);
        }
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

    private List<CourseLookupResponse> validateCoursesExist(Set<UUID> courseIds, String authorizationHeader) {
        try {
            if (isTeacherAuthenticated()) {
                return courseIds.stream()
                        .map(courseId -> lookupCourseById(courseId, authorizationHeader))
                        .toList();
            }

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

            return courses;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "ids", courseIds);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate courses", ex);
        }
    }

    private boolean isTeacherAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_TEACHER".equals(authority) || "TEACHER".equals(authority));
    }

    private void assertTeacherCanManageCourses(List<CourseLookupResponse> courses) {
        if (!isTeacherAuthenticated()) {
            return;
        }

        UUID teacherId = currentUserService.getCurrentUserId();
        boolean ownsAllCourses = courses.stream()
                .allMatch(course -> teacherId.equals(course.getAssignedTeacherId()));
        if (!ownsAllCourses) {
            throw new AccessDeniedException("Teacher can only manage enrollments for assigned courses");
        }
    }

    private List<CourseLookupResponse> lookupCoursesForStudent(Set<UUID> courseIds, String authorizationHeader) {
        return courseIds.stream()
                .map(courseId -> lookupCourseById(courseId, authorizationHeader))
                .toList();
    }

    private CourseLookupResponse lookupCourseById(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<CourseLookupResponse> response = courseServiceClient.lookupCourseById(courseId, authorizationHeader);
            CourseLookupResponse course = response != null ? response.getData() : null;
            if (course == null || course.getId() == null) {
                throw new ResourceNotFoundException("Course", "id", courseId);
            }
            return course;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate course " + courseId, ex);
        }
    }

    private void validateSuccessfulPaymentsForPaidCourses(
            UUID studentId,
            List<CourseLookupResponse> courses,
            Set<UUID> courseIdsToValidate,
            String authorizationHeader
    ) {
        courses.stream()
                .filter(course -> courseIdsToValidate.contains(course.getId()))
                .filter(this::isPaidCourse)
                .forEach(course -> validateSuccessfulPayment(studentId, course.getId(), authorizationHeader));
    }

    private boolean isPaidCourse(CourseLookupResponse course) {
        BigDecimal monthlyFee = course.getGradeMonthlyFee();
        return monthlyFee != null && monthlyFee.compareTo(BigDecimal.ZERO) > 0;
    }

    private void validateSuccessfulPayment(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<PaymentVerificationResponse> response =
                    paymentServiceClient.verifySuccessfulPayment(studentId, courseId, authorizationHeader);
            PaymentVerificationResponse verification = response != null ? response.getData() : null;

            if (verification == null
                    || !studentId.equals(verification.getStudentId())
                    || !courseId.equals(verification.getCourseId())
                    || !verification.isSuccessfulPaymentExists()) {
                throw new PaymentRequiredException(
                        "Successful payment is required before enrolling in paid course " + courseId
                );
            }
        } catch (FeignException.Forbidden ex) {
            throw new AccessDeniedException("Payment verification is not allowed", ex);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to verify payment for course " + courseId, ex);
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

    private ApiResponse<EntrollmentResponse> saveEnrollment(
            UUID studentId,
            Set<UUID> courseIds,
            String authorizationHeader,
            boolean strictCommunicationSync,
            EnrollmentStatus status
    ) {
        Entrollment entrollment = Entrollment.builder()
                .studentId(studentId)
                .courseIds(courseIds)
                .enrolledOn(LocalDate.now())
                .status(status)
                .build();

        Entrollment savedEntrollment = entrollmentRepository.save(entrollment);
        syncStudentToCommunicationGroups(
                savedEntrollment.getStudentId(),
                savedEntrollment.getCourseIds(),
                authorizationHeader,
                strictCommunicationSync
        );

        return ApiResponse.<EntrollmentResponse>builder()
                .message("Enrollment created successfully")
                .data(toResponse(savedEntrollment))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    private void syncStudentToCommunicationGroups(
            UUID studentId,
            Set<UUID> courseIds,
            String authorizationHeader,
            boolean strictCommunicationSync
    ) {
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
            } catch (FeignException.Forbidden ex) {
                if (strictCommunicationSync) {
                    throw new IllegalStateException("Failed to add student to communication group for course " + courseId, ex);
                }
                log.info("Student [{}] enrolled in course [{}]; communication group sync requires elevated permissions", studentId, courseId);
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
                .status(entrollment.getStatus() != null ? entrollment.getStatus() : EnrollmentStatus.APPROVED)
                .build();
    }
}
