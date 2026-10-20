package com.mentify.material.service.impl;

import com.mentify.material.dto.request.MaterialAssociationRequest;
import com.mentify.material.dto.response.MaterialAssociationResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.MaterialAssociationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialAssociationServiceImpl implements MaterialAssociationService {

    private final MaterialRepository materialRepository;
    private final MaterialAuthorizationService authorizationService;

    @Override
    @Transactional
    public MaterialAssociationResponse associate(
            UUID materialId,
            MaterialAssociationRequest request,
            String authorizationHeader
    ) {
        Material material = load(materialId);
        if (request == null) {
            throw new MaterialValidationException("Material association is required");
        }
        authorizationService.assertCanAssociate(
                material,
                request.resourceType(),
                request.resourceId(),
                request.courseId(),
                request.ownerId(),
                request.visibility(),
                authorizationHeader
        );
        material.setResourceType(request.resourceType());
        material.setResourceId(request.resourceId());
        material.setCourseId(request.courseId() == null ? material.getCourseId() : request.courseId());
        material.setOwnerId(request.ownerId());
        material.setVisibility(request.visibility());
        return toResponse(materialRepository.saveAndFlush(material));
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialAssociationResponse getAssociation(UUID materialId, String authorizationHeader) {
        Material material = load(materialId);
        authorizationService.assertCanView(material, authorizationHeader);
        return toResponse(material);
    }

    private Material load(UUID materialId) {
        if (materialId == null) {
            throw new MaterialNotFoundException("Material not found");
        }
        return materialRepository.findById(materialId)
                .filter(material -> material.isActive()
                        && material.getStatus() != com.mentify.material.enums.MaterialStatus.DELETED)
                .orElseThrow(() -> new MaterialNotFoundException("Material not found"));
    }

    private MaterialAssociationResponse toResponse(Material material) {
        return new MaterialAssociationResponse(
                material.getId(),
                material.getResourceType(),
                material.getResourceId(),
                material.getCourseId(),
                material.getOwnerId(),
                material.getVisibility()
        );
    }
}
