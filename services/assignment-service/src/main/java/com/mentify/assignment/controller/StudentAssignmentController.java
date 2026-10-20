package com.mentify.assignment.controller;

import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.service.StudentAssignmentService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Validated
public class StudentAssignmentController {

    private final StudentAssignmentService studentAssignmentService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Page<StudentAssignmentResponse>>> getMyAssignments(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime dueFrom,
            @RequestParam(required = false) LocalDateTime dueTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader("Authorization") String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt) {
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new IllegalArgumentException("dueFrom must be before or equal to dueTo");
        }
        UUID studentId = UUID.fromString(jwt.getSubject());
        Page<StudentAssignmentResponse> assignments = studentAssignmentService.getAssignments(
                studentId, courseId, status, dueFrom, dueTo, page, size, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(200, "Student assignments fetched successfully", assignments));
    }
}
