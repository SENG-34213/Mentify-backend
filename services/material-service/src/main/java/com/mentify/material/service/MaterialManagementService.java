package com.mentify.material.service;

import com.mentify.material.dto.request.UpdateMaterialRequest;
import com.mentify.material.dto.request.MaterialSearchCriteria;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MaterialManagementService {

    Page<MaterialDetailsResponse> listMaterials(
            MaterialSearchCriteria criteria,
            String authorizationHeader,
            Pageable pageable
    );

    MaterialDetailsResponse updateMaterial(UUID materialId, UpdateMaterialRequest request, String authorizationHeader);

    MaterialDetailsResponse archiveMaterial(UUID materialId, String authorizationHeader);

    MaterialDetailsResponse deleteMaterial(UUID materialId, String authorizationHeader);
}
