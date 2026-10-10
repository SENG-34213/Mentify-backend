package com.mentify.repository;

import com.mentify.dto.AdminPaymentFilter;
import com.mentify.entity.Payment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<Payment> adminFilter(AdminPaymentFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getStudentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentId"), filter.getStudentId()));
            }

            if (filter.getCourseId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("courseId"), filter.getCourseId()));
            }

            if (filter.getCreatedFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }

            if (filter.getCreatedTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }

            if (StringUtils.hasText(filter.getReference())) {
                String reference = filter.getReference().trim();
                Predicate stripePaymentIntentMatch = criteriaBuilder.equal(root.get("stripePaymentIntentId"), reference);
                parseUuid(reference)
                        .map(paymentId -> criteriaBuilder.or(
                                criteriaBuilder.equal(root.get("id"), paymentId),
                                stripePaymentIntentMatch
                        ))
                        .ifPresentOrElse(predicates::add, () -> predicates.add(stripePaymentIntentMatch));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
