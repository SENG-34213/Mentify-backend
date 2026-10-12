package com.mentify.material.controller;

import com.mentify.material.dto.request.MaterialCleanupRequest;
import com.mentify.material.dto.response.MaterialCleanupResponse;
import com.mentify.material.service.MaterialCleanupService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/materials/maintenance")
public class MaterialMaintenanceController {

    private final MaterialCleanupService materialCleanupService;

    @PostMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialCleanupResponse>> audit(
            @RequestBody(required = false) MaterialCleanupRequest request
    ) {
        MaterialCleanupResponse response = materialCleanupService.audit(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material storage audit completed", response));
    }

    @PostMapping("/cleanup")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialCleanupResponse>> cleanup(
            @RequestBody(required = false) MaterialCleanupRequest request
    ) {
        MaterialCleanupResponse response = materialCleanupService.cleanup(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material storage cleanup completed", response));
    }
}
