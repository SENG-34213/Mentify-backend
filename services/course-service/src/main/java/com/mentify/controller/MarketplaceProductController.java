package com.mentify.controller;

import com.mentify.dto.MarketplaceProductRequest;
import com.mentify.dto.MarketplaceProductResponse;
import com.mentify.enums.MarketplaceProductStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.MarketplaceProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MarketplaceProductController {

    private final MarketplaceProductService service;

    @GetMapping("/api/v1/marketplace/materials")
    public ResponseEntity<ApiResponse<Page<MarketplaceProductResponse>>> listPublic(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) UUID gradeId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace materials retrieved",
                service.listPublic(search, subject, gradeId, pageable)));
    }

    @GetMapping("/api/v1/marketplace/materials/{productId}")
    public ResponseEntity<ApiResponse<MarketplaceProductResponse>> getPublic(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace material retrieved",
                service.getPublic(productId)));
    }

    @GetMapping("/api/v1/marketplace/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<MarketplaceProductResponse>>> listAdmin(
            @RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace products retrieved",
                service.listAdmin(search, pageable)));
    }

    @PostMapping("/api/v1/marketplace/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MarketplaceProductResponse>> create(@Valid @RequestBody MarketplaceProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(),
                "Marketplace product created", service.create(request)));
    }

    @PutMapping("/api/v1/marketplace/admin/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MarketplaceProductResponse>> update(
            @PathVariable UUID productId, @Valid @RequestBody MarketplaceProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace product updated",
                service.update(productId, request)));
    }

    @PatchMapping("/api/v1/marketplace/admin/products/{productId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MarketplaceProductResponse>> changeStatus(
            @PathVariable UUID productId, @RequestParam MarketplaceProductStatus status) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace product status updated",
                service.changeStatus(productId, status)));
    }

    @DeleteMapping("/api/v1/marketplace/admin/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable UUID productId) {
        service.delete(productId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Marketplace product deleted", null));
    }
}
