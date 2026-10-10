package com.mentify.repository;

import com.mentify.entity.Payment;
import com.mentify.enums.PaymentProvider;
import com.mentify.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindPayment_persistsPaymentDetails() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDateTime paidAt = LocalDateTime.now().withNano(0);

        Payment payment = Payment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .amount(new BigDecimal("1499.99"))
                .currency("LKR")
                .status(PaymentStatus.SUCCESS)
                .provider(PaymentProvider.STRIPE)
                .stripePaymentIntentId("pi_test_payment_001")
                .stripeClientSecret("pi_test_payment_001_secret")
                .paymentOperationKey(UUID.randomUUID().toString())
                .paidAt(paidAt)
                .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        entityManager.clear();

        Payment foundPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();

        assertThat(foundPayment.getStudentId()).isEqualTo(studentId);
        assertThat(foundPayment.getCourseId()).isEqualTo(courseId);
        assertThat(foundPayment.getAmount()).isEqualByComparingTo("1499.99");
        assertThat(foundPayment.getCurrency()).isEqualTo("LKR");
        assertThat(foundPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(foundPayment.getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(foundPayment.getStripePaymentIntentId()).isEqualTo("pi_test_payment_001");
        assertThat(foundPayment.getStripeClientSecret()).isEqualTo("pi_test_payment_001_secret");
        assertThat(foundPayment.getPaidAt()).isEqualTo(paidAt);
        assertThat(foundPayment.getCreatedAt()).isNotNull();
        assertThat(foundPayment.getUpdatedAt()).isNotNull();
    }

    @Test
    void savePayment_withoutExplicitStatusAndProvider_usesPendingStripeDefaults() {
        Payment payment = Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        entityManager.clear();

        Payment foundPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();

        assertThat(foundPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(foundPayment.getProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    void findByStripePaymentIntentId_returnsMatchingPayment() {
        Payment payment = Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("250.50"))
                .currency("USD")
                .stripePaymentIntentId("pi_lookup_001")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();
        paymentRepository.saveAndFlush(payment);
        entityManager.clear();

        assertThat(paymentRepository.findByStripePaymentIntentId("pi_lookup_001"))
                .isPresent()
                .get()
                .extracting(Payment::getStripePaymentIntentId)
                .isEqualTo("pi_lookup_001");
        assertThat(paymentRepository.existsByStripePaymentIntentId("pi_lookup_001")).isTrue();
    }

    @Test
    void findByIdAndStudentId_returnsOnlyOwnedPayment() {
        UUID studentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .studentId(studentId)
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("250.50"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build());
        entityManager.clear();

        assertThat(paymentRepository.findByIdAndStudentId(payment.getId(), studentId)).isPresent();
        assertThat(paymentRepository.findByIdAndStudentId(payment.getId(), otherStudentId)).isEmpty();
    }

    @Test
    void findAllByStudentIdOrderByCreatedAtDesc_returnsMostRecentFirst() {
        UUID studentId = UUID.randomUUID();
        Payment olderPayment = Payment.builder()
                .studentId(studentId)
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();
        olderPayment.setCreatedAt(LocalDateTime.of(2026, 10, 9, 10, 0));
        olderPayment.setUpdatedAt(LocalDateTime.of(2026, 10, 9, 10, 0));
        paymentRepository.saveAndFlush(olderPayment);

        Payment newestPayment = Payment.builder()
                .studentId(studentId)
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();
        newestPayment.setCreatedAt(LocalDateTime.of(2026, 10, 10, 10, 0));
        newestPayment.setUpdatedAt(LocalDateTime.of(2026, 10, 10, 10, 0));
        paymentRepository.saveAndFlush(newestPayment);
        entityManager.clear();

        assertThat(paymentRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId))
                .extracting(Payment::getId)
                .containsExactly(newestPayment.getId(), olderPayment.getId());
    }

    @Test
    void savePayment_whenStripePaymentIntentIdDuplicated_throwsDataIntegrityViolationException() {
        String stripePaymentIntentId = "pi_duplicate_001";

        paymentRepository.saveAndFlush(Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .stripePaymentIntentId(stripePaymentIntentId)
                .paymentOperationKey(UUID.randomUUID().toString())
                .build());
        entityManager.clear();

        Payment duplicatePayment = Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .stripePaymentIntentId(stripePaymentIntentId)
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(duplicatePayment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savePayments_whenStripePaymentIntentIdIsNull_allowsMultipleRecords() {
        paymentRepository.saveAndFlush(Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build());

        Payment secondPayment = paymentRepository.saveAndFlush(Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .paymentOperationKey(UUID.randomUUID().toString())
                .build());

        assertThat(secondPayment.getId()).isNotNull();
    }
}
