package com.mentify.service.impl;

import com.mentify.dto.PaymentAnalyticsOverviewResponse;
import com.mentify.entity.Payment;
import com.mentify.enums.PaymentStatus;
import com.mentify.repository.PaymentRepository;
import com.mentify.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsServiceImpl implements PaymentAnalyticsService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentAnalyticsOverviewResponse getOverview() {
        var payments = paymentRepository.findAll().stream().filter(Payment::isActive).toList();
        BigDecimal revenue = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentAnalyticsOverviewResponse(
                revenue,
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS).count(),
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count(),
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED).count());
    }
}
