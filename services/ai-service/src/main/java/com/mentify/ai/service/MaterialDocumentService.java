package com.mentify.ai.service;

import com.mentify.ai.dto.internal.MaterialDocument;

import java.util.UUID;

public interface MaterialDocumentService {
    MaterialDocument loadMaterial(UUID materialId, String authorizationHeader);
}
