package com.mentify.service.impl;

import com.mentify.client.CourseServiceClient;
import com.mentify.client.dto.CourseBulkLookupRequest;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.dto.EntrollmentUpdateRequest;
import com.mentify.entity.Entrollment;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.EntrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrollmentServiceImplTest {

    @Mock
    private EntrollmentRepository entrollmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @InjectMocks
    private EntrollmentServiceImpl entrollmentService;

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

    private ApiResponse<List<CourseLookupResponse>> successCourseLookupResponse(List<UUID> courseIds) {
        List<CourseLookupResponse> responses = courseIds.stream().map(id -> {
            CourseLookupResponse response = new CourseLookupResponse();
            response.setId(id);
            return response;
        }).toList();

        return ApiResponse.<List<CourseLookupResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Courses fetched successfully")
                .data(responses)
                .build();
    }
}

