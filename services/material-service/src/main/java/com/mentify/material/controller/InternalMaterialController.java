package com.mentify.material.controller;

import com.mentify.material.dto.request.MaterialAssociationRequest;
import com.mentify.material.dto.response.MaterialAssociationResponse;
import com.mentify.material.service.MaterialAssociationService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/materials")
public class InternalMaterialController {

    private final MaterialAssociationService materialAssociationService;

    @PostMapping("/{materialId}/association")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialAssociationResponse>> associate(
            @PathVariable UUID materialId,
            @RequestBody MaterialAssociationRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialAssociationResponse response = materialAssociationService.associate(materialId, request, authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Material association saved successfully", response));
    }

    @GetMapping("/{materialId}/association")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialAssociationResponse>> getAssociation(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        MaterialAssociationResponse response = materialAssociationService.getAssociation(materialId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Material association retrieved successfully", response));
    }
}
