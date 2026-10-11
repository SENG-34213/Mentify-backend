package com.mentify.material.repository;

import com.mentify.material.dto.request.MaterialSearchCriteria;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class MaterialSpecifications {

    private MaterialSpecifications() {
    }

    public static Specification<Material> visibleFilter(MaterialSearchCriteria criteria) {
        MaterialSearchCriteria safeCriteria = criteria == null
                ? new MaterialSearchCriteria(null, null, null, null, null, null, null, null, null, null, null)
                : criteria;

        return Specification
                .where(isActive())
                .and(notDeleted())
                .and(matchesSearch(safeCriteria.search()))
                .and(safeCriteria.type() == null ? null : (root, query, cb) -> cb.equal(root.get("type"), safeCriteria.type()))
                .and(safeCriteria.purpose() == null ? null : (root, query, cb) -> cb.equal(root.get("purpose"), safeCriteria.purpose()))
                .and(safeCriteria.status() == null ? null : (root, query, cb) -> cb.equal(root.get("status"), safeCriteria.status()))
                .and(safeCriteria.uploadedBy() == null ? null : (root, query, cb) -> cb.equal(root.get("uploadedBy"), safeCriteria.uploadedBy()))
                .and(safeCriteria.courseId() == null ? null : (root, query, cb) -> cb.equal(root.get("courseId"), safeCriteria.courseId()))
                .and(safeCriteria.createdFrom() == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), safeCriteria.createdFrom()))
                .and(safeCriteria.createdTo() == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), safeCriteria.createdTo()))
                .and(hasMaterialIds(safeCriteria));
    }

    private static Specification<Material> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    private static Specification<Material> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), MaterialStatus.DELETED);
    }

    private static Specification<Material> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("originalFilename")), pattern, '\\')
        );
    }

    private static Specification<Material> hasMaterialIds(MaterialSearchCriteria criteria) {
        if (criteria.materialIds() == null) {
            return null;
        }
        return criteria.materialIds().isEmpty()
                ? (root, query, cb) -> cb.disjunction()
                : (root, query, cb) -> root.get("id").in(criteria.materialIds());
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
