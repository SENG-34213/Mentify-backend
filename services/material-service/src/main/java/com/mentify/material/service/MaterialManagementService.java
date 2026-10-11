package com.mentify.material.service;

import com.mentify.material.dto.request.UpdateMaterialRequest;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MaterialManagementService {

    Page<MaterialDetailsResponse> listMaterials(
            MaterialType type,
            MaterialPurpose purpose,
            MaterialStatus status,
            UUID uploadedBy,
            UUID courseId,
            String authorizationHeader,
            Pageable pageable
    );

    MaterialDetailsResponse updateMaterial(UUID materialId, UpdateMaterialRequest request, String authorizationHeader);

    MaterialDetailsResponse archiveMaterial(UUID materialId, String authorizationHeader);

    MaterialDetailsResponse deleteMaterial(UUID materialId, String authorizationHeader);
}
