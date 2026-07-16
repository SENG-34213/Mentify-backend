package com.mentify.controller;

import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.EntrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EntrollmentController {

    private final EntrollmentService entrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EntrollmentResponse>> createEntrollment(
            @Valid @RequestBody EntrollmentCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<EntrollmentResponse> response =
                entrollmentService.createEntrollment(request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}

