package com.mentify.material.repository;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class MaterialSpecifications {

    private MaterialSpecifications() {
    }

    public static Specification<Material> visibleFilter(
            MaterialType type,
            MaterialPurpose purpose,
            MaterialStatus status,
            UUID uploadedBy
    ) {
        return Specification
                .where(isActive())
                .and(notDeleted())
                .and(hasType(type))
                .and(hasPurpose(purpose))
                .and(hasStatus(status))
                .and(hasUploadedBy(uploadedBy));
    }

    private static Specification<Material> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    private static Specification<Material> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), MaterialStatus.DELETED);
    }

    private static Specification<Material> hasType(MaterialType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Material> hasPurpose(MaterialPurpose purpose) {
        return purpose == null ? null : (root, query, cb) -> cb.equal(root.get("purpose"), purpose);
    }

    private static Specification<Material> hasStatus(MaterialStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Material> hasUploadedBy(UUID uploadedBy) {
        return uploadedBy == null ? null : (root, query, cb) -> cb.equal(root.get("uploadedBy"), uploadedBy);
    }
}
