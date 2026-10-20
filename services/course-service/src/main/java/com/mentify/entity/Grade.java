package com.mentify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "grades",
        uniqueConstraints = @UniqueConstraint(name = "uk_grades_name", columnNames = "name")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "LKR";
}
