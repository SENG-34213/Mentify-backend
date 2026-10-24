package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private String courseCode;

    private BigDecimal gradeMonthlyFee;

    /** @deprecated Course pricing is now owned by the grade. */
    @Deprecated
    @JsonIgnore
    private BigDecimal courseFeeMonthly;

    public CourseLookupResponse(UUID id, String courseName, BigDecimal courseFeeMonthly,
                                boolean published, boolean visible) {
        this.id = id;
        this.courseName = courseName;
        this.courseFeeMonthly = courseFeeMonthly;
        this.gradeMonthlyFee = courseFeeMonthly;
        this.published = published;
        this.visible = visible;
    }

    @JsonAlias({"published", "isPublished"})
    private boolean published;

    private boolean visible;
}
