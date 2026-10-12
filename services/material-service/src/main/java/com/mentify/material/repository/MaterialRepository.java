package com.mentify.material.repository;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    Optional<Material> findByStorageKey(String storageKey);

    boolean existsByStorageKey(String storageKey);

    List<Material> findAllByUploadedByOrderByCreatedAtDesc(UUID uploadedBy);

    List<Material> findAllByStatusOrderByCreatedAtDesc(MaterialStatus status);

    List<Material> findAllByPurposeAndStatusOrderByCreatedAtDesc(MaterialPurpose purpose, MaterialStatus status);

    List<Material> findAllByChecksum(String checksum);

    @Query("select m.storageKey from Material m where m.storageKey is not null")
    List<String> findAllStorageKeys();

    @Query("""
            select m
            from Material m
            where m.storageKey is not null
              and m.status <> com.mentify.material.enums.MaterialStatus.DELETED
            order by m.createdAt asc
            """)
    List<Material> findStorageTrackedMaterials();

    @Query("""
            select m
            from Material m
            where m.status = com.mentify.material.enums.MaterialStatus.FAILED
               or (m.status = com.mentify.material.enums.MaterialStatus.PROCESSING and m.updatedAt <= :cutoff)
            order by m.updatedAt asc
            """)
    List<Material> findFailedOrIncompleteMaterials(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            select m
            from Material m
            where m.status = com.mentify.material.enums.MaterialStatus.DELETED
              and m.isActive = false
              and m.updatedAt <= :cutoff
            order by m.updatedAt asc
            """)
    List<Material> findPurgeableDeletedMaterials(@Param("cutoff") LocalDateTime cutoff);
}
