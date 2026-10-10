package com.mentify.material.dto;

import com.mentify.material.enums.MaterialType;

public record ValidatedMaterialFile(
        String originalFilename,
        String sanitizedFilename,
        String extension,
        String contentType,
        long fileSize,
        MaterialType materialType
) {
}
