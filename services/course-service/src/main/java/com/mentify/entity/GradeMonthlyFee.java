package com.mentify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "grade_monthly_fees", uniqueConstraints =
        @UniqueConstraint(name = "uk_grade_monthly_fees_grade_id", columnNames = "grade_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeMonthlyFee extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_id", nullable = false, unique = true)
    private Grade grade;

    @Column(name = "monthly_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "LKR";
}
