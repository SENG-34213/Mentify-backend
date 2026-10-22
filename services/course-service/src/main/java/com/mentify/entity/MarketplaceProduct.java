package com.mentify.entity;

import com.mentify.enums.MarketplaceProductCategory;
import com.mentify.enums.MarketplaceProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_products", indexes = {
        @Index(name = "idx_marketplace_products_status", columnList = "status"),
        @Index(name = "idx_marketplace_products_category", columnList = "category"),
        @Index(name = "idx_marketplace_products_grade", columnList = "grade_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceProduct extends BaseEntity {

    @Column(name = "material_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID materialId;

    @Column(name = "thumbnail_material_id", columnDefinition = "uuid")
    private UUID thumbnailMaterialId;

    @Column(name = "material_owner_id", columnDefinition = "uuid")
    private UUID materialOwnerId;

    @Column(name = "material_owner_name", length = 180)
    private String materialOwnerName;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MarketplaceProductCategory category;

    @Column(length = 120)
    private String subject;

    @Column(name = "course_domain", length = 120)
    private String courseDomain;

    @Column(name = "grade_id", columnDefinition = "uuid")
    private UUID gradeId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private boolean free = false;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "LKR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MarketplaceProductStatus status = MarketplaceProductStatus.DRAFT;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    private LocalDateTime publishedAt;
}
