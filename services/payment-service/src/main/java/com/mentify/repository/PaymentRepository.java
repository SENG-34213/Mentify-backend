package com.mentify.repository;

import com.mentify.entity.Payment;
import com.mentify.enums.PaymentStatus;
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

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);
}
