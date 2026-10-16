package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseLookupResponse {

    private UUID id;

    private String courseName;

    private BigDecimal courseFeeMonthly;

    @JsonAlias({"published", "isPublished"})
    private boolean published;

    private boolean visible;
}
