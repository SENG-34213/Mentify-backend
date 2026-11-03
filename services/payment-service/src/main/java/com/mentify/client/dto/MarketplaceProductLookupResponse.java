package com.mentify.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MarketplaceProductLookupResponse {
    private UUID id;
    private String title;
    private BigDecimal finalPrice;
    private BigDecimal price;
    private boolean free;
    private String currency;
    private String status;
}
