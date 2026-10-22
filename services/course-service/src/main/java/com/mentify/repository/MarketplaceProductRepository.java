package com.mentify.repository;

import com.mentify.entity.MarketplaceProduct;
import com.mentify.enums.MarketplaceProductCategory;
import com.mentify.enums.MarketplaceProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MarketplaceProductRepository extends JpaRepository<MarketplaceProduct, UUID>,
        JpaSpecificationExecutor<MarketplaceProduct> {

    Page<MarketplaceProduct> findAllByStatus(MarketplaceProductStatus status, Pageable pageable);

    Page<MarketplaceProduct> findAllByStatusAndCategory(
            MarketplaceProductStatus status,
            MarketplaceProductCategory category,
            Pageable pageable
    );
}
