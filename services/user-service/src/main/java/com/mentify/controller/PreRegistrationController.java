package com.mentify.controller;

import com.mentify.dto.PreRegistrationRequest;
import com.mentify.dto.PreRegistrationResponse;
import com.mentify.dto.PreRegistrationUpdateRequest;
import com.mentify.enums.PreRegistrationStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PreRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PreRegistrationController {
    private final PreRegistrationService service;

    @Value("${mentify.pre-registration.api-token:}")
    private String submissionToken;

    @PostMapping("/public/pre-registrations")
    public ResponseEntity<ApiResponse<PreRegistrationResponse>> create(
            @RequestHeader(value = "X-Pre-Registration-Token", required = false) String requestToken,
            @Valid @RequestBody PreRegistrationRequest request) {
        if (submissionToken.isBlank() || requestToken == null || !MessageDigest.isEqual(
                submissionToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.success(
                    HttpStatus.FORBIDDEN.value(), "Pre-registration submission is not authorized", null));
        }
        PreRegistrationResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(), "Pre-registration submitted successfully", response));
    }

    @GetMapping("/admin/pre-registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PreRegistrationResponse>>> findAll(
            @RequestParam(required = false) PreRegistrationStatus status,
            @RequestParam(required = false) Boolean seen,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<PreRegistrationResponse> response = service.findAll(status, seen, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Pre-registrations fetched successfully", response));
    }

    @PatchMapping("/admin/pre-registrations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PreRegistrationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PreRegistrationUpdateRequest request) {
        PreRegistrationResponse response = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Pre-registration updated successfully", response));
    }
}