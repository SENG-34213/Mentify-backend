package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.service.GradingService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GradingController {

    private final GradingService gradingService;

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = gradingService.gradeSubmission(submissionId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/submissions/{submissionId}/return")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> returnSubmission(
            @PathVariable UUID submissionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionResponse> response = gradingService.returnSubmission(submissionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
