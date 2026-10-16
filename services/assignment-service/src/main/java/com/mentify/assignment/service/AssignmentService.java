package com.mentify.assignment.service;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {
    ApiResponse<AssignmentResponse> createAssignment(CreateAssignmentRequest request, String authorizationHeader);
    ApiResponse<AssignmentResponse> updateAssignment(UUID assignmentId, UpdateAssignmentRequest request, String authorizationHeader);
    ApiResponse<AssignmentResponse> getAssignment(UUID assignmentId);
    ApiResponse<List<AssignmentResponse>> listAssignmentsForCourse(UUID courseId, String authorizationHeader);
    ApiResponse<Object> deleteAssignment(UUID assignmentId, String authorizationHeader);
    ApiResponse<AssignmentResponse> publishAssignment(UUID assignmentId, String authorizationHeader);
    ApiResponse<AssignmentResponse> closeAssignment(UUID assignmentId, String authorizationHeader);
}
