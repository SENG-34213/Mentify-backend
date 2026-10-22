package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.CreateSubmissionRequest;
import com.mentify.assignment.dto.request.UpdateSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.service.SubmissionService;
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
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> createSubmission(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CreateSubmissionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = submissionService.createSubmission(assignmentId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> updateSubmission(
            @PathVariable UUID submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = submissionService.updateSubmission(submissionId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/submissions/{submissionId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID submissionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = submissionService.submitAssignment(submissionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(
            @PathVariable UUID submissionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = submissionService.getSubmission(submissionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> listSubmissionsForAssignment(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<SubmissionResponse>> response = submissionService.listSubmissionsForAssignment(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/submissions/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> listMySubmissions(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<SubmissionResponse>> response = submissionService.listMySubmissions(authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
