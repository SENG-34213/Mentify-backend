package com.mentify.ai.service.impl;

import com.mentify.ai.client.MaterialServiceClient;
import com.mentify.ai.client.dto.MaterialDetailsResponse;
import com.mentify.ai.client.dto.MaterialDownloadResponse;
import com.mentify.ai.dto.internal.MaterialDocument;
import com.mentify.ai.exception.AiInvalidDocumentException;
import com.mentify.ai.service.MaterialDocumentService;
import com.mentify.ai.service.MaterialDownloadClient;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialDocumentServiceImpl implements MaterialDocumentService {

    private static final Set<String> AVAILABLE_STATUSES = Set.of("UPLOADED", "AVAILABLE");
    private static final Set<String> SUPPORTED_TYPES = Set.of("PDF", "DOCUMENT");

    private final MaterialServiceClient materialServiceClient;
    private final MaterialDownloadClient materialDownloadClient;

    @Override
    public MaterialDocument loadMaterial(UUID materialId, String authorizationHeader) {
        MaterialDetailsResponse details = getMaterial(materialId, authorizationHeader);
        validateMaterial(details);

        MaterialDownloadResponse download = createDownload(materialId, authorizationHeader);
        byte[] content = materialDownloadClient.download(download.downloadUrl());
        if (content == null || content.length == 0) {
            throw new AiInvalidDocumentException("Material file is empty");
        }

        return new MaterialDocument(
                details.id(),
                details.title(),
                details.originalFilename(),
                details.contentType(),
                details.fileSize(),
                details.type(),
                details.status(),
                details.uploadedBy(),
                details.courseId(),
                content
        );
    }

    private MaterialDetailsResponse getMaterial(UUID materialId, String authorizationHeader) {
        try {
            ApiResponse<MaterialDetailsResponse> response = materialServiceClient.getMaterial(materialId, authorizationHeader);
            if (response == null || response.getData() == null || response.getData().id() == null) {
                throw new AiInvalidDocumentException("Material not found");
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new AiInvalidDocumentException("Material not found", ex);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material access denied", ex);
        }
    }

    private MaterialDownloadResponse createDownload(UUID materialId, String authorizationHeader) {
        try {
            ApiResponse<MaterialDownloadResponse> response = materialServiceClient.createDownload(materialId, authorizationHeader);
            if (response == null || response.getData() == null || response.getData().downloadUrl() == null) {
                throw new AiInvalidDocumentException("Material download URL is not available");
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new AiInvalidDocumentException("Material not found", ex);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material access denied", ex);
        }
    }

    private void validateMaterial(MaterialDetailsResponse material) {
        if (!AVAILABLE_STATUSES.contains(normalize(material.status()))) {
            throw new AiInvalidDocumentException("Material is not available for AI processing");
        }
        if (!SUPPORTED_TYPES.contains(normalize(material.type()))) {
            throw new AiInvalidDocumentException("Unsupported material type for AI processing");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
