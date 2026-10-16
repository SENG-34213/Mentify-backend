package com.mentify.material.service;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;

import java.util.UUID;

public interface MaterialRetrievalService {

    MaterialDetailsResponse getMaterial(UUID materialId, String authorizationHeader);

    MaterialDownloadResponse createDownload(UUID materialId, String authorizationHeader);
}
