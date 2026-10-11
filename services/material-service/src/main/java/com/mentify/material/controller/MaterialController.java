package com.mentify.material.controller;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.security.CurrentMaterialUserService;
import com.mentify.material.service.MaterialRetrievalService;
import com.mentify.material.service.MaterialUploadService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialUploadService materialUploadService;
    private final MaterialRetrievalService materialRetrievalService;
    private final CurrentMaterialUserService currentUserService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> uploadMaterial(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "LEARNING_MATERIAL") MaterialPurpose purpose,
            @RequestParam MultipartFile file
    ) {
        MaterialResponse response = materialUploadService.upload(
                title,
                description,
                purpose,
                file,
                currentUserService.getCurrentUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Material uploaded successfully", response));
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> getMaterial(@PathVariable UUID materialId) {
        MaterialDetailsResponse response = materialRetrievalService.getMaterial(materialId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material retrieved successfully", response));
    }

    @GetMapping("/{materialId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDownloadResponse>> createDownload(@PathVariable UUID materialId) {
        MaterialDownloadResponse response = materialRetrievalService.createDownload(materialId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material download URL generated", response));
    }
}
