package com.mentify.repository;

import com.mentify.dto.AdminPaymentFilter;
import com.mentify.entity.Payment;
import com.mentify.enums.PaymentProvider;
import com.mentify.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    void existsByStudentIdAndCourseIdAndStatus_returnsOnlyMatchingSuccessfulPayment() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        persistedPayment(
                studentId,
                courseId,
                PaymentStatus.SUCCESS,
                "pi_successful_for_enrollment",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );
        persistedPayment(
                studentId,
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_successful_other_course",
                LocalDateTime.of(2026, 10, 10, 11, 0)
        );
        persistedPayment(
                UUID.randomUUID(),
                courseId,
                PaymentStatus.SUCCESS,
                "pi_successful_other_student",
                LocalDateTime.of(2026, 10, 10, 12, 0)
        );
        persistedPayment(
                studentId,
                courseId,
                PaymentStatus.FAILED,
                "pi_failed_same_student_course",
                LocalDateTime.of(2026, 10, 10, 13, 0)
        );
        entityManager.clear();

        assertThat(paymentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, PaymentStatus.SUCCESS))
                .isTrue();
        assertThat(paymentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, PaymentStatus.PENDING))
                .isFalse();
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
    void adminFilter_filtersByStatusStudentCourseAndDateRange() {
        UUID matchingStudentId = UUID.randomUUID();
        UUID matchingCourseId = UUID.randomUUID();
        Payment matchingPayment = persistedPayment(
                matchingStudentId,
                matchingCourseId,
                PaymentStatus.SUCCESS,
                "pi_filter_match",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );
        persistedPayment(
                matchingStudentId,
                matchingCourseId,
                PaymentStatus.PENDING,
                "pi_filter_wrong_status",
                LocalDateTime.of(2026, 10, 10, 11, 0)
        );
        persistedPayment(
                UUID.randomUUID(),
                matchingCourseId,
                PaymentStatus.SUCCESS,
                "pi_filter_wrong_student",
                LocalDateTime.of(2026, 10, 10, 12, 0)
        );
        persistedPayment(
                matchingStudentId,
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_filter_wrong_course",
                LocalDateTime.of(2026, 10, 10, 13, 0)
        );
        entityManager.clear();

        Page<Payment> result = paymentRepository.findAll(
                PaymentSpecifications.adminFilter(AdminPaymentFilter.builder()
                        .status(PaymentStatus.SUCCESS)
                        .studentId(matchingStudentId)
                        .courseId(matchingCourseId)
                        .createdFrom(LocalDateTime.of(2026, 10, 10, 9, 0))
                        .createdTo(LocalDateTime.of(2026, 10, 10, 10, 30))
                        .build()),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Payment::getId)
                .containsExactly(matchingPayment.getId());
    }

    @Test
    void adminFilter_filtersBySafeReferenceForStripePaymentIntentOrPaymentId() {
        Payment stripeReferencedPayment = persistedPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.PENDING,
                "pi_reference_lookup",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );
        Payment idReferencedPayment = persistedPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_other_reference",
                LocalDateTime.of(2026, 10, 10, 11, 0)
        );
        entityManager.clear();

        Page<Payment> stripeReferenceResult = paymentRepository.findAll(
                PaymentSpecifications.adminFilter(AdminPaymentFilter.builder()
                        .reference("pi_reference_lookup")
                        .build()),
                PageRequest.of(0, 10)
        );
        Page<Payment> paymentIdReferenceResult = paymentRepository.findAll(
                PaymentSpecifications.adminFilter(AdminPaymentFilter.builder()
                        .reference(idReferencedPayment.getId().toString())
                        .build()),
                PageRequest.of(0, 10)
        );

        assertThat(stripeReferenceResult.getContent())
                .extracting(Payment::getId)
                .containsExactly(stripeReferencedPayment.getId());
        assertThat(paymentIdReferenceResult.getContent())
                .extracting(Payment::getId)
                .containsExactly(idReferencedPayment.getId());
    }

    @Test
    void adminPaymentQuery_supportsPaginationAndMostRecentFirstOrdering() {
        Payment oldestPayment = persistedPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_admin_oldest",
                LocalDateTime.of(2026, 10, 8, 10, 0)
        );
        Payment middlePayment = persistedPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_admin_middle",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );
        Payment newestPayment = persistedPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentStatus.SUCCESS,
                "pi_admin_newest",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );
        entityManager.clear();

        Page<Payment> firstPage = paymentRepository.findAll(
                PaymentSpecifications.adminFilter(AdminPaymentFilter.builder()
                        .status(PaymentStatus.SUCCESS)
                        .build()),
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );
        Page<Payment> secondPage = paymentRepository.findAll(
                PaymentSpecifications.adminFilter(AdminPaymentFilter.builder()
                        .status(PaymentStatus.SUCCESS)
                        .build()),
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent())
                .extracting(Payment::getId)
                .containsExactly(newestPayment.getId(), middlePayment.getId());
        assertThat(secondPage.getContent())
                .extracting(Payment::getId)
                .containsExactly(oldestPayment.getId());
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

    private Payment persistedPayment(
            UUID studentId,
            UUID courseId,
            PaymentStatus status,
            String stripePaymentIntentId,
            LocalDateTime createdAt
    ) {
        Payment payment = Payment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(status)
                .stripePaymentIntentId(stripePaymentIntentId)
                .paymentOperationKey(UUID.randomUUID().toString())
                .build();
        payment.setCreatedAt(createdAt);
        payment.setUpdatedAt(createdAt);
        return paymentRepository.saveAndFlush(payment);
    }
}
