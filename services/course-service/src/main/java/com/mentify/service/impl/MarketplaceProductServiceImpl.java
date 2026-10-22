package com.mentify.service.impl;

import com.mentify.dto.MarketplaceProductRequest;
import com.mentify.dto.MarketplaceProductResponse;
import com.mentify.entity.MarketplaceProduct;
import com.mentify.enums.MarketplaceProductStatus;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.MarketplaceProductRepository;
import com.mentify.service.MarketplaceProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketplaceProductServiceImpl implements MarketplaceProductService {

    private final MarketplaceProductRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<MarketplaceProductResponse> listPublic(String search, String subject, UUID gradeId, Pageable pageable) {
        Specification<MarketplaceProduct> specification = Specification.where(
                (root, query, cb) -> cb.equal(root.get("status"), MarketplaceProductStatus.PUBLISHED));
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("shortDescription")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern)));
        }
        if (subject != null && !subject.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("subject"), subject));
        }
        if (gradeId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("gradeId"), gradeId));
        }
        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceProductResponse getPublic(UUID productId) {
        MarketplaceProduct product = repository.findById(productId)
                .filter(item -> item.getStatus() == MarketplaceProductStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace product not found"));
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MarketplaceProductResponse> listAdmin(String search, Pageable pageable) {
        Specification<MarketplaceProduct> specification = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern)));
        }
        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public MarketplaceProductResponse create(MarketplaceProductRequest request) {
        MarketplaceProduct product = new MarketplaceProduct();
        apply(product, request);
        return toResponse(repository.save(product));
    }

    @Override
    @Transactional
    public MarketplaceProductResponse update(UUID productId, MarketplaceProductRequest request) {
        MarketplaceProduct product = repository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace product not found"));
        apply(product, request);
        return toResponse(repository.save(product));
    }

    @Override
    @Transactional
    public MarketplaceProductResponse changeStatus(UUID productId, MarketplaceProductStatus status) {
        MarketplaceProduct product = repository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace product not found"));
        product.setStatus(status);
        product.setPublishedAt(status == MarketplaceProductStatus.PUBLISHED
                ? (product.getPublishedAt() == null ? LocalDateTime.now() : product.getPublishedAt()) : product.getPublishedAt());
        return toResponse(repository.save(product));
    }

    private void apply(MarketplaceProduct product, MarketplaceProductRequest request) {
        product.setMaterialId(request.materialId());
        product.setThumbnailMaterialId(request.thumbnailMaterialId());
        product.setMaterialOwnerId(request.materialOwnerId());
        product.setMaterialOwnerName(request.materialOwnerName());
        product.setTitle(request.title().trim());
        product.setShortDescription(request.shortDescription());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setSubject(request.subject());
        product.setCourseDomain(request.courseDomain());
        product.setGradeId(request.gradeId());
        product.setPrice(request.price());
        product.setFree(request.free());
        product.setDiscountPercent(request.free() ? null : request.discountPercent());
        if (request.free()) {
            product.setPrice(BigDecimal.ZERO);
        } else if (request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid marketplace products must have a price greater than zero");
        }
        product.setCurrency(request.currency() == null || request.currency().isBlank() ? "LKR" : request.currency().toUpperCase());
        product.setThumbnailUrl(request.thumbnailUrl());
        if (request.status() != null) product.setStatus(request.status());
    }

    private MarketplaceProductResponse toResponse(MarketplaceProduct product) {
        BigDecimal finalPrice = product.isFree() ? BigDecimal.ZERO
                : product.getPrice().multiply(BigDecimal.ONE.subtract(
                        (product.getDiscountPercent() == null ? BigDecimal.ZERO : product.getDiscountPercent())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        return new MarketplaceProductResponse(product.getId(), product.getMaterialId(), product.getThumbnailMaterialId(),
                product.getMaterialOwnerId(), product.getMaterialOwnerName(), product.getTitle(), product.getShortDescription(),
                product.getDescription(), product.getCategory(), product.getSubject(), product.getCourseDomain(),
                product.getGradeId(), product.getPrice(), product.isFree(), product.getDiscountPercent(), finalPrice,
                product.getCurrency(), product.getStatus(), product.getThumbnailUrl(),
                product.getPublishedAt(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
