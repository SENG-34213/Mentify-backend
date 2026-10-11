package com.mentify.material.controller;

import com.mentify.material.dto.request.UpdateMaterialRequest;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.security.CurrentMaterialUserService;
import com.mentify.material.service.MaterialManagementService;
import com.mentify.material.service.MaterialRetrievalService;
import com.mentify.material.service.MaterialUploadService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final MaterialManagementService materialManagementService;
    private final CurrentMaterialUserService currentUserService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> uploadMaterial(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "LEARNING_MATERIAL") MaterialPurpose purpose,
            @RequestParam UUID courseId,
            @RequestParam MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialResponse response = materialUploadService.upload(
                title,
                description,
                purpose,
                file,
                currentUserService.getCurrentUserId(),
                courseId,
                authorizationHeader
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Material uploaded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<MaterialDetailsResponse>>> listMaterials(
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) MaterialPurpose purpose,
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestParam(required = false) UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MaterialDetailsResponse> response = materialManagementService.listMaterials(
                type,
                purpose,
                status,
                uploadedBy,
                courseId,
                authorizationHeader,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Materials retrieved successfully", response));
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> getMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialDetailsResponse response = materialRetrievalService.getMaterial(materialId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material retrieved successfully", response));
    }

    @GetMapping("/{materialId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDownloadResponse>> createDownload(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialDownloadResponse response = materialRetrievalService.createDownload(materialId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material download URL generated", response));
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> updateMaterial(
            @PathVariable UUID materialId,
            @RequestBody UpdateMaterialRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialDetailsResponse response = materialManagementService.updateMaterial(materialId, request, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material updated successfully", response));
    }

    @PostMapping("/{materialId}/archive")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> archiveMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialDetailsResponse response = materialManagementService.archiveMaterial(materialId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material archived successfully", response));
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> deleteMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialDetailsResponse response = materialManagementService.deleteMaterial(materialId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material deleted successfully", response));
    }
}
