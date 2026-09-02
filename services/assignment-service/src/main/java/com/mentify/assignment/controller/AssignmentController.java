package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.createAssignment(request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.updateAssignment(assignmentId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(@PathVariable UUID assignmentId) {
        ApiResponse<AssignmentResponse> response = assignmentService.getAssignment(assignmentId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/courses/{courseId}/assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignmentsForCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<AssignmentResponse>> response = assignmentService.listAssignmentsForCourse(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<Object> response = assignmentService.deleteAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/assignments/{assignmentId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publishAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.publishAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/assignments/{assignmentId}/close")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> closeAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentResponse> response = assignmentService.closeAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
