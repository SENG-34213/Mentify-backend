package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseLookupResponse {
    private UUID id;
    private BigDecimal courseFeeMonthly;
}
