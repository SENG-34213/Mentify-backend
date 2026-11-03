package com.mentify.service;

import com.mentify.dto.MarketplaceProductRequest;
import com.mentify.dto.MarketplaceProductResponse;
import com.mentify.enums.MarketplaceProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MarketplaceProductService {
    Page<MarketplaceProductResponse> listPublic(String search, String subject, UUID gradeId,
            MarketplaceProductCategory category, Boolean isFree, Pageable pageable);
    MarketplaceProductResponse getPublic(UUID productId);
    Page<MarketplaceProductResponse> listAdmin(String search, Pageable pageable);
    MarketplaceProductResponse create(MarketplaceProductRequest request);
    MarketplaceProductResponse update(UUID productId, MarketplaceProductRequest request);
    MarketplaceProductResponse changeStatus(UUID productId, com.mentify.enums.MarketplaceProductStatus status);
    void delete(UUID productId);
}
