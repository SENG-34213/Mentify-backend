package com.mentify.entity;

import com.mentify.enums.PaymentProvider;
import com.mentify.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_stripe_payment_intent_id", columnNames = "stripe_payment_intent_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @NotNull
    @Column(nullable = false, columnDefinition = "uuid")
    private UUID studentId;

    @NotNull
    @Column(nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false, message = "Payment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Payment amount must have at most 10 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.STRIPE;

    @Size(max = 255)
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Size(max = 500)
    @Column(name = "stripe_client_secret", length = 500)
    private String stripeClientSecret;

    @NotNull
    @Size(max = 255)
    @Column(name = "payment_operation_key", nullable = false, length = 255)
    private String paymentOperationKey;

    private LocalDateTime paidAt;

    @PrePersist
    @PreUpdate
    protected void ensurePaymentOperationKey() {
        if (paymentOperationKey == null
                && studentId != null
                && courseId != null
                && currency != null
                && amount != null) {
            paymentOperationKey = studentId + "|" + courseId + "|" + currency + "|" + amount.setScale(2).toPlainString();
        }
    }
}
