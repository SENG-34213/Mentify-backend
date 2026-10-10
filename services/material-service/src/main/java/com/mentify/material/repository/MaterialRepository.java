package com.mentify.material.repository;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    Optional<Material> findByStorageKey(String storageKey);

    boolean existsByStorageKey(String storageKey);

    List<Material> findAllByUploadedByOrderByCreatedAtDesc(UUID uploadedBy);

    List<Material> findAllByStatusOrderByCreatedAtDesc(MaterialStatus status);

    List<Material> findAllByPurposeAndStatusOrderByCreatedAtDesc(MaterialPurpose purpose, MaterialStatus status);

    List<Material> findAllByChecksum(String checksum);
}
