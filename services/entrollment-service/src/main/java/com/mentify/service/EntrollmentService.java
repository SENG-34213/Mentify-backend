package com.mentify.service;

import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.dto.EntrollmentUpdateRequest;
import com.mentify.dto.StudentEntrollmentCreateRequest;
import com.mentify.dto.UnenrolledStudentResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface EntrollmentService {

    ApiResponse<EntrollmentResponse> createEntrollment(EntrollmentCreateRequest request, String authorizationHeader);

    ApiResponse<EntrollmentResponse> createCurrentStudentEntrollment(StudentEntrollmentCreateRequest request, String authorizationHeader);

    ApiResponse<EntrollmentResponse> updateEntrollment(UUID enrollmentId, EntrollmentUpdateRequest request, String authorizationHeader);

    boolean isStudentEnrolledInCourse(UUID studentId, UUID courseId);

    ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(UUID courseId);

    ApiResponse<List<EntrollmentResponse>> getActiveEntrollments();

    ApiResponse<List<UnenrolledStudentResponse>> getUnenrolledStudents(String authorizationHeader);
}
