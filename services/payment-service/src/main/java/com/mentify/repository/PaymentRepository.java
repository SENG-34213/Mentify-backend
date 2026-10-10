package com.mentify.repository;

import com.mentify.entity.Payment;
import com.mentify.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<Payment> findAllByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Payment> findAllByStatusOrderByCreatedAtDesc(PaymentStatus status);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findByIdAndStudentId(UUID id, UUID studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findFirstByPaymentOperationKeyAndStatusOrderByCreatedAtDesc(
            String paymentOperationKey,
            PaymentStatus status
    );

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);
}
