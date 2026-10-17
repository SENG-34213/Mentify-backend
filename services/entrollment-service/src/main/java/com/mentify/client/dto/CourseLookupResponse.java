package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseLookupResponse {
    private UUID id;
    private BigDecimal gradeMonthlyFee;

    /** @deprecated Course pricing is now owned by the grade. */
    @Deprecated
    @JsonIgnore
    private BigDecimal courseFeeMonthly;
}
