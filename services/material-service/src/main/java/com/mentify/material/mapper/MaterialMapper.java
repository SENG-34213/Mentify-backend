package com.mentify.material.mapper;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.entity.Material;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    public MaterialDetailsResponse toDetailsResponse(Material material) {
        return new MaterialDetailsResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSize(),
                material.getType(),
                material.getPurpose(),
                material.getStatus(),
                material.getUploadedBy(),
                material.getChecksum(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }

    public MaterialDownloadResponse toDownloadResponse(Material material, S3DownloadResult download) {
        return new MaterialDownloadResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSize(),
                material.getType(),
                material.getPurpose(),
                material.getStatus(),
                material.getUploadedBy(),
                material.getChecksum(),
                material.getCreatedAt(),
                material.getUpdatedAt(),
                download.downloadUrl(),
                download.expiresAt()
        );
    }
}
