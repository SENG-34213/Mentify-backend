package com.mentify.dto;

import com.mentify.enums.MarketplaceProductCategory;
import com.mentify.enums.MarketplaceProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record MarketplaceProductRequest(
        @NotNull UUID materialId,
        UUID thumbnailMaterialId,
        UUID materialOwnerId,
        @Size(max = 180) String materialOwnerName,
        @NotBlank @Size(max = 180) String title,
        @Size(max = 500) String shortDescription,
        String description,
        @NotNull MarketplaceProductCategory category,
        @Size(max = 120) String subject,
        @Size(max = 120) String courseDomain,
        UUID gradeId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        boolean free,
        @DecimalMin(value = "0.00") @DecimalMax(value = "100.00") BigDecimal discountPercent,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 500) String thumbnailUrl,
        MarketplaceProductStatus status
) {
}
