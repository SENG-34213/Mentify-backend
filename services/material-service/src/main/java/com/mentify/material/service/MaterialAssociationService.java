package com.mentify.material.service;

import com.mentify.material.dto.request.MaterialAssociationRequest;
import com.mentify.material.dto.response.MaterialAssociationResponse;

import java.util.UUID;

public interface MaterialAssociationService {

    MaterialAssociationResponse associate(UUID materialId, MaterialAssociationRequest request, String authorizationHeader);

    MaterialAssociationResponse getAssociation(UUID materialId, String authorizationHeader);
}
