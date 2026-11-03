package com.mentify.service.impl;

import com.mentify.dto.PaymentAnalyticsOverviewResponse;
import com.mentify.enums.PaymentStatus;
import com.mentify.repository.PaymentRepository;
import com.mentify.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsServiceImpl implements PaymentAnalyticsService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentAnalyticsOverviewResponse getOverview() {
        // The admin payment list is not restricted by the soft-delete flag. Keep the
        // aggregate consistent with that list so successful payments are not silently
        // omitted from the revenue card.
        var payments = paymentRepository.findAll();
        YearMonth currentMonth = YearMonth.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, BigDecimal> revenueTrend = new LinkedHashMap<>();
        for (int offset = 11; offset >= 0; offset--) {
            revenueTrend.put(currentMonth.minusMonths(offset).format(monthFormatter), BigDecimal.ZERO);
        }
        BigDecimal revenue = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .forEach(payment -> {
                    var paymentDate = payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt();
                    if (paymentDate == null) return;
                    String month = YearMonth.from(paymentDate).format(monthFormatter);
                    if (revenueTrend.containsKey(month)) {
                        revenueTrend.merge(month, payment.getAmount(), BigDecimal::add);
                    }
                });
        return new PaymentAnalyticsOverviewResponse(
                revenue,
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS).count(),
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count(),
                payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED).count(),
                revenueTrend);
    }
}
