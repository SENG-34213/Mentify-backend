package com.mentify.dto;

import com.mentify.enums.MarketplaceProductCategory;
import com.mentify.enums.MarketplaceProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MarketplaceProductResponse(
        UUID id,
        UUID materialId,
        UUID thumbnailMaterialId,
        UUID materialOwnerId,
        String materialOwnerName,
        String title,
        String shortDescription,
        String description,
        MarketplaceProductCategory category,
        String subject,
        String courseDomain,
        UUID gradeId,
        BigDecimal price,
        boolean free,
        BigDecimal discountPercent,
        BigDecimal finalPrice,
        String currency,
        MarketplaceProductStatus status,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
