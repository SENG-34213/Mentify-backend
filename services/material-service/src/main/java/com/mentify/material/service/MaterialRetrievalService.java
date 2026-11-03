package com.mentify.material.service;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;

import java.util.UUID;

public interface MaterialRetrievalService {

    MaterialDetailsResponse getMaterial(UUID materialId, String authorizationHeader);

    MaterialDownloadResponse createDownload(UUID materialId, String authorizationHeader);

    S3DownloadResult createPublicThumbnail(UUID materialId);
}
