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
import com.mentify.dto.StudentEntrollmentCreateRequest;
import com.mentify.dto.UnenrolledStudentResponse;
import com.mentify.entity.Entrollment;
import com.mentify.exception.PaymentRequiredException;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.EntrollmentRepository;
import com.mentify.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrollmentServiceImplTest {

    @Mock
    private EntrollmentRepository entrollmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private CommunicationServiceClient communicationServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EntrollmentServiceImpl entrollmentService;

    @Test
    void createEnrollmentSyncsStudentToAllCourseCommunicationGroups() {
        UUID studentId = UUID.randomUUID();
        UUID courseOne = UUID.randomUUID();
        UUID courseTwo = UUID.randomUUID();
        UUID courseThree = UUID.randomUUID();

        EntrollmentCreateRequest request = EntrollmentCreateRequest.builder()
                .studentId(studentId)
                .courseIds(List.of(courseOne, courseTwo, courseThree))
                .build();

        when(courseServiceClient.getCoursesByIds(any(CourseBulkLookupRequest.class), eq("Bearer token")))
                .thenReturn(successCourseLookupResponse(List.of(courseOne, courseTwo, courseThree)));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<EntrollmentResponse> response = entrollmentService.createEntrollment(request, "Bearer token");

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(Set.of(courseOne, courseTwo, courseThree), response.getData().getCourseIds());
        verify(communicationServiceClient).addStudentToCourseGroup(eq(courseOne), any(AddStudentToGroupRequest.class), eq("Bearer token"));
        verify(communicationServiceClient).addStudentToCourseGroup(eq(courseTwo), any(AddStudentToGroupRequest.class), eq("Bearer token"));
        verify(communicationServiceClient).addStudentToCourseGroup(eq(courseThree), any(AddStudentToGroupRequest.class), eq("Bearer token"));
    }

    @Test
    void createCurrentStudentEnrollment_whenPaidCourseHasSuccessfulPayment_succeeds() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        StudentEntrollmentCreateRequest request = StudentEntrollmentCreateRequest.builder()
                .courseIds(List.of(courseId))
                .build();

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer student-token"))
                .thenReturn(successPaymentVerification(studentId, courseId, true));
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<EntrollmentResponse> response =
                entrollmentService.createCurrentStudentEntrollment(request, "Bearer student-token");

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(studentId, response.getData().getStudentId());
        assertEquals(Set.of(courseId), response.getData().getCourseIds());
        verify(paymentServiceClient).verifySuccessfulPayment(studentId, courseId, "Bearer student-token");
        verify(entrollmentRepository).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenPaidCoursePaymentIsPending_doesNotEnroll() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer student-token"))
                .thenReturn(successPaymentVerification(studentId, courseId, false));

        assertThrows(
                PaymentRequiredException.class,
                () -> entrollmentService.createCurrentStudentEntrollment(
                        StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                        "Bearer student-token"
                )
        );

        verify(entrollmentRepository, never()).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenPaidCoursePaymentIsFailed_doesNotEnroll() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer student-token"))
                .thenReturn(successPaymentVerification(studentId, courseId, false));

        assertThrows(
                PaymentRequiredException.class,
                () -> entrollmentService.createCurrentStudentEntrollment(
                        StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                        "Bearer student-token"
                )
        );

        verify(entrollmentRepository, never()).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenPaymentBelongsToAnotherCourse_doesNotEnroll() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID otherCourseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer student-token"))
                .thenReturn(successPaymentVerification(studentId, otherCourseId, true));

        assertThrows(
                PaymentRequiredException.class,
                () -> entrollmentService.createCurrentStudentEntrollment(
                        StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                        "Bearer student-token"
                )
        );

        verify(entrollmentRepository, never()).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenPaymentBelongsToAnotherStudent_doesNotEnroll() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer student-token"))
                .thenReturn(successPaymentVerification(otherStudentId, courseId, true));

        assertThrows(
                PaymentRequiredException.class,
                () -> entrollmentService.createCurrentStudentEntrollment(
                        StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                        "Bearer student-token"
                )
        );

        verify(entrollmentRepository, never()).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenDuplicateEnrollmentExists_doesNotCreateDuplicate() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Entrollment existing = Entrollment.builder()
                .studentId(studentId)
                .courseIds(Set.of(courseId))
                .build();

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "1499.99"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of(existing));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> entrollmentService.createCurrentStudentEntrollment(
                        StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                        "Bearer student-token"
                )
        );

        verify(paymentServiceClient, never()).verifySuccessfulPayment(any(), any(), any());
        verify(entrollmentRepository, never()).save(any(Entrollment.class));
    }

    @Test
    void createCurrentStudentEnrollment_whenCourseIsFree_doesNotRequirePayment() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.lookupCourseById(courseId, "Bearer student-token"))
                .thenReturn(successCourseLookupResponse(courseId, "0.00"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<EntrollmentResponse> response = entrollmentService.createCurrentStudentEntrollment(
                StudentEntrollmentCreateRequest.builder().courseIds(List.of(courseId)).build(),
                "Bearer student-token"
        );

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(Set.of(courseId), response.getData().getCourseIds());
        verify(paymentServiceClient, never()).verifySuccessfulPayment(any(), any(), any());
    }

    @Test
    void createEnrollment_whenAdminEnrollsPaidCourse_requiresSuccessfulPayment() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        EntrollmentCreateRequest request = EntrollmentCreateRequest.builder()
                .studentId(studentId)
                .courseIds(List.of(courseId))
                .build();

        when(courseServiceClient.getCoursesByIds(any(CourseBulkLookupRequest.class), eq("Bearer admin-token")))
                .thenReturn(successCourseLookupResponse(List.of(courseId), "2500.00"));
        when(entrollmentRepository.findAllByStudentIdAndIsActiveTrue(studentId)).thenReturn(List.of());
        when(paymentServiceClient.verifySuccessfulPayment(studentId, courseId, "Bearer admin-token"))
                .thenReturn(successPaymentVerification(studentId, courseId, true));
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<EntrollmentResponse> response = entrollmentService.createEntrollment(request, "Bearer admin-token");

        assertEquals(HttpStatus.CREATED, response.getStatus());
        verify(paymentServiceClient).verifySuccessfulPayment(studentId, courseId, "Bearer admin-token");
    }

    @Test
    void updateEnrollmentSucceedsForValidRequest() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseOne = UUID.randomUUID();
        UUID courseTwo = UUID.randomUUID();

        Entrollment existing = Entrollment.builder()
                .studentId(studentId)
                .courseIds(Set.of(UUID.randomUUID()))
                .enrolledOn(LocalDate.now())
                .build();
        existing.setId(enrollmentId);

        EntrollmentUpdateRequest request = EntrollmentUpdateRequest.builder()
                .courseIds(List.of(courseOne, courseTwo))
                .build();

        when(entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)).thenReturn(Optional.of(existing));
        when(courseServiceClient.getCoursesByIds(any(CourseBulkLookupRequest.class), eq("Bearer token")))
                .thenReturn(successCourseLookupResponse(List.of(courseOne, courseTwo)));
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<EntrollmentResponse> response =
                entrollmentService.updateEntrollment(enrollmentId, request, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("Enrollment updated successfully", response.getMessage());
        assertEquals(Set.of(courseOne, courseTwo), response.getData().getCourseIds());

        ArgumentCaptor<Entrollment> captor = ArgumentCaptor.forClass(Entrollment.class);
        verify(entrollmentRepository, times(1)).save(captor.capture());
        assertEquals(Set.of(courseOne, courseTwo), captor.getValue().getCourseIds());
        verify(communicationServiceClient).addStudentToCourseGroup(eq(courseOne), any(AddStudentToGroupRequest.class), eq("Bearer token"));
        verify(communicationServiceClient).addStudentToCourseGroup(eq(courseTwo), any(AddStudentToGroupRequest.class), eq("Bearer token"));
    }

    @Test
    void updateEnrollmentSyncsOnlyNewCourses() {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID existingCourse = UUID.randomUUID();
        UUID newCourse = UUID.randomUUID();

        Entrollment existing = Entrollment.builder()
                .studentId(studentId)
                .courseIds(Set.of(existingCourse))
                .enrolledOn(LocalDate.now())
                .build();
        existing.setId(enrollmentId);

        EntrollmentUpdateRequest request = EntrollmentUpdateRequest.builder()
                .courseIds(List.of(existingCourse, newCourse))
                .build();

        when(entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)).thenReturn(Optional.of(existing));
        when(courseServiceClient.getCoursesByIds(any(CourseBulkLookupRequest.class), eq("Bearer token")))
                .thenReturn(successCourseLookupResponse(List.of(existingCourse, newCourse)));
        when(entrollmentRepository.save(any(Entrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        entrollmentService.updateEntrollment(enrollmentId, request, "Bearer token");

        verify(communicationServiceClient).addStudentToCourseGroup(eq(newCourse), any(AddStudentToGroupRequest.class), eq("Bearer token"));
        verify(communicationServiceClient, times(0)).addStudentToCourseGroup(eq(existingCourse), any(AddStudentToGroupRequest.class), any());
    }

    @Test
    void updateEnrollmentFailsWhenEnrollmentDoesNotExist() {
        UUID enrollmentId = UUID.randomUUID();

        when(entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> entrollmentService.updateEntrollment(
                        enrollmentId,
                        EntrollmentUpdateRequest.builder().courseIds(List.of(UUID.randomUUID())).build(),
                        "Bearer token"
                )
        );
    }

    @Test
    void updateEnrollmentFailsWhenAnyCourseDoesNotExist() {
        UUID enrollmentId = UUID.randomUUID();
        UUID missingCourseId = UUID.randomUUID();

        Entrollment existing = Entrollment.builder()
                .studentId(UUID.randomUUID())
                .courseIds(Set.of(UUID.randomUUID()))
                .enrolledOn(LocalDate.now())
                .build();
        existing.setId(enrollmentId);

        when(entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)).thenReturn(Optional.of(existing));
        when(courseServiceClient.getCoursesByIds(any(CourseBulkLookupRequest.class), eq("Bearer token")))
                .thenReturn(successCourseLookupResponse(List.of()));

        assertThrows(
                ResourceNotFoundException.class,
                () -> entrollmentService.updateEntrollment(
                        enrollmentId,
                        EntrollmentUpdateRequest.builder().courseIds(List.of(missingCourseId)).build(),
                        "Bearer token"
                )
        );
    }

    @Test
    void updateEnrollmentFailsWhenDuplicateCourseIdsAreProvided() {
        UUID enrollmentId = UUID.randomUUID();
        UUID duplicateCourseId = UUID.randomUUID();

        Entrollment existing = Entrollment.builder()
                .studentId(UUID.randomUUID())
                .courseIds(Set.of(UUID.randomUUID()))
                .enrolledOn(LocalDate.now())
                .build();
        existing.setId(enrollmentId);

        when(entrollmentRepository.findByIdAndIsActiveTrue(enrollmentId)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> entrollmentService.updateEntrollment(
                        enrollmentId,
                        EntrollmentUpdateRequest.builder().courseIds(List.of(duplicateCourseId, duplicateCourseId)).build(),
                        "Bearer token"
                )
        );

        assertTrue(ex.getMessage().contains("Duplicate course IDs"));
        verify(courseServiceClient, times(0)).getCoursesByIds(any(CourseBulkLookupRequest.class), any());
    }

    @Test
    void getEnrolledStudentIdsByCourseReturnsActiveStudents() {
        UUID courseId = UUID.randomUUID();
        UUID firstStudentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();

        when(entrollmentRepository.findActiveStudentIdsByCourseId(courseId))
                .thenReturn(List.of(firstStudentId, secondStudentId));

        ApiResponse<List<UUID>> response = entrollmentService.getEnrolledStudentIdsByCourse(courseId);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(List.of(firstStudentId, secondStudentId), response.getData());
    }

    @Test
    void getUnenrolledStudentsReturnsStudentsWithNoActiveEnrollment() {
        UUID enrolledStudentId = UUID.randomUUID();
        UUID unenrolledStudentId = UUID.randomUUID();

        when(entrollmentRepository.findActiveStudentIds()).thenReturn(List.of(enrolledStudentId));
        when(userServiceClient.getUsersByRole("STUDENT", "Bearer token"))
                .thenReturn(ApiResponse.<List<UserLookupResponse>>builder()
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .message("Users fetched successfully")
                        .data(List.of(
                                student(enrolledStudentId, "enrolled@example.com", "Enrolled", "Student"),
                                student(unenrolledStudentId, "free@example.com", "Free", "Student")
                        ))
                        .build());

        ApiResponse<List<UnenrolledStudentResponse>> response = entrollmentService.getUnenrolledStudents("Bearer token");

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(1, response.getData().size());
        assertEquals(unenrolledStudentId, response.getData().get(0).getId());
        assertEquals("free@example.com", response.getData().get(0).getEmail());
    }

    private ApiResponse<List<CourseLookupResponse>> successCourseLookupResponse(List<UUID> courseIds) {
        return successCourseLookupResponse(courseIds, null);
    }

    private ApiResponse<List<CourseLookupResponse>> successCourseLookupResponse(List<UUID> courseIds, String courseFeeMonthly) {
        List<CourseLookupResponse> responses = courseIds.stream().map(id -> {
            CourseLookupResponse response = new CourseLookupResponse();
            response.setId(id);
            if (courseFeeMonthly != null) {
                response.setCourseFeeMonthly(new BigDecimal(courseFeeMonthly));
            }
            return response;
        }).toList();

        return ApiResponse.<List<CourseLookupResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Courses fetched successfully")
                .data(responses)
                .build();
    }

    private ApiResponse<CourseLookupResponse> successCourseLookupResponse(UUID courseId, String courseFeeMonthly) {
        CourseLookupResponse response = new CourseLookupResponse();
        response.setId(courseId);
        response.setCourseFeeMonthly(new BigDecimal(courseFeeMonthly));

        return ApiResponse.<CourseLookupResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Course fetched successfully")
                .data(response)
                .build();
    }

    private ApiResponse<PaymentVerificationResponse> successPaymentVerification(
            UUID studentId,
            UUID courseId,
            boolean successfulPaymentExists
    ) {
        PaymentVerificationResponse response = new PaymentVerificationResponse();
        response.setStudentId(studentId);
        response.setCourseId(courseId);
        response.setSuccessfulPaymentExists(successfulPaymentExists);

        return ApiResponse.<PaymentVerificationResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Payment verification completed")
                .data(response)
                .build();
    }

    private UserLookupResponse student(UUID id, String email, String firstName, String lastName) {
        UserLookupResponse student = new UserLookupResponse();
        student.setId(id);
        student.setEmail(email);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setRole("STUDENT");
        student.setAccountStatus("ACTIVE");

        UserLookupResponse.StudentProfileResponse profile = new UserLookupResponse.StudentProfileResponse();
        profile.setStudentId("STU-" + id.toString().substring(0, 8));
        profile.setGrade("10");
        student.setStudentProfile(profile);

        return student;
    }
}
