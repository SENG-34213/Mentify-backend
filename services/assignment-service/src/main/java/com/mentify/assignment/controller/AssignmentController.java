package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/api/assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.createAssignment(request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/api/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response =
                assignmentService.updateAssignment(assignmentId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getTeacherAssignment(@PathVariable UUID assignmentId) {
        ApiResponse<AssignmentResponse> response = assignmentService.getTeacherAssignment(assignmentId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/api/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteAssignment(@PathVariable UUID assignmentId) {
        ApiResponse<Object> response = assignmentService.deleteAssignment(assignmentId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/api/assignments/{assignmentId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publishAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.publishAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/api/assignments/{assignmentId}/close")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> closeAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.closeAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentAssignmentResponse>>> getPublishedAssignmentsByCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<StudentAssignmentResponse>> response =
                assignmentService.getPublishedAssignmentsByCourse(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/api/assignments/{assignmentId}/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentAssignmentResponse>> getStudentAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<StudentAssignmentResponse> response =
                assignmentService.getStudentAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
