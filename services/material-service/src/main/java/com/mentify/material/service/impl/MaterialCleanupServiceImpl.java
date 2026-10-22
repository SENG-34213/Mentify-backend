package com.mentify.material.service.impl;

import com.mentify.material.dto.request.MaterialCleanupRequest;
import com.mentify.material.dto.response.MaterialCleanupFailureResponse;
import com.mentify.material.dto.response.MaterialCleanupResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.exception.MaterialStorageException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.MaterialAuditService;
import com.mentify.material.service.MaterialCleanupService;
import com.mentify.material.service.MaterialStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialCleanupServiceImpl implements MaterialCleanupService {

    private static final int DEFAULT_DELETED_RETENTION_DAYS = 30;
    private static final int DEFAULT_INCOMPLETE_UPLOAD_AGE_HOURS = 24;

    private final MaterialRepository materialRepository;
    private final MaterialStorageService storageService;
    private final MaterialAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public MaterialCleanupResponse audit(MaterialCleanupRequest request) {
        MaterialCleanupResponse response = inspect(request, true);
        auditService.recordCleanup("SUCCESS", summary("audit", response));
        return response;
    }

    @Override
    @Transactional
    public MaterialCleanupResponse cleanup(MaterialCleanupRequest request) {
        boolean dryRun = request == null || request.dryRun() == null || request.dryRun();
        MaterialCleanupResponse response = inspect(request, dryRun);
        auditService.recordCleanup(response.failures().isEmpty() ? "SUCCESS" : "PARTIAL_FAILURE", summary("cleanup", response));
        return response;
    }

    private MaterialCleanupResponse inspect(MaterialCleanupRequest request, boolean dryRun) {
        int deletedRetentionDays = positiveOrDefault(
                request != null ? request.deletedRetentionDays() : null,
                DEFAULT_DELETED_RETENTION_DAYS
        );
        int incompleteUploadAgeHours = positiveOrDefault(
                request != null ? request.incompleteUploadAgeHours() : null,
                DEFAULT_INCOMPLETE_UPLOAD_AGE_HOURS
        );

        List<String> knownKeys = materialRepository.findAllStorageKeys();
        Set<String> knownKeySet = new HashSet<>(knownKeys);
        List<String> orphanKeys = storageService.listStorageKeys().stream()
                // Profile images are owned by user-service, not material rows.
                .filter(key -> !key.startsWith("materials/profile-images/"))
                .filter(key -> !knownKeySet.contains(key))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<Material> missingObjects = materialRepository.findStorageTrackedMaterials().stream()
                .filter(material -> !objectExists(material))
                .toList();
        List<Material> failedOrIncomplete = materialRepository.findFailedOrIncompleteMaterials(
                now.minusHours(incompleteUploadAgeHours)
        );
        List<Material> purgeableDeleted = materialRepository.findPurgeableDeletedMaterials(
                now.minusDays(deletedRetentionDays)
        );

        CleanupCounters counters = new CleanupCounters();
        List<MaterialCleanupFailureResponse> failures = new java.util.ArrayList<>();

        if (!dryRun) {
            cleanupOrphans(orphanKeys, counters, failures);
            purgeDeletedMaterials(purgeableDeleted, counters, failures);
        }

        return new MaterialCleanupResponse(
                dryRun,
                orphanKeys.size(),
                missingObjects.size(),
                failedOrIncomplete.size(),
                purgeableDeleted.size(),
                counters.orphanObjectsDeleted,
                counters.materialsPurged,
                orphanKeys.stream().map(this::keyHash).toList(),
                missingObjects.stream().map(Material::getId).toList(),
                failedOrIncomplete.stream().map(Material::getId).toList(),
                purgeableDeleted.stream().map(Material::getId).toList(),
                failures
        );
    }

    private boolean objectExists(Material material) {
        try {
            return storageService.exists(material.getStorageKey());
        } catch (MaterialStorageException ex) {
            log.warn("Skipping material storage existence result materialId={} reason={}", material.getId(), ex.getClass().getSimpleName());
            return false;
        }
    }

    private void cleanupOrphans(
            List<String> orphanKeys,
            CleanupCounters counters,
            List<MaterialCleanupFailureResponse> failures
    ) {
        for (String orphanKey : orphanKeys) {
            try {
                storageService.delete(orphanKey);
                counters.orphanObjectsDeleted++;
            } catch (RuntimeException ex) {
                failures.add(new MaterialCleanupFailureResponse(null, keyHash(orphanKey), "DELETE_ORPHAN_OBJECT", ex.getClass().getSimpleName()));
            }
        }
    }

    private void purgeDeletedMaterials(
            List<Material> purgeableDeleted,
            CleanupCounters counters,
            List<MaterialCleanupFailureResponse> failures
    ) {
        for (Material material : purgeableDeleted) {
            try {
                if (storageService.exists(material.getStorageKey())) {
                    storageService.delete(material.getStorageKey());
                }
                materialRepository.delete(material);
                counters.materialsPurged++;
            } catch (RuntimeException ex) {
                failures.add(new MaterialCleanupFailureResponse(
                        material.getId(),
                        keyHash(material.getStorageKey()),
                        "PURGE_DELETED_MATERIAL",
                        ex.getClass().getSimpleName()
                ));
            }
        }
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String keyHash(String storageKey) {
        return storageKey == null ? null : Integer.toHexString(storageKey.hashCode());
    }

    private String summary(String operation, MaterialCleanupResponse response) {
        return "operation=" + operation
                + " dryRun=" + response.dryRun()
                + " orphanObjectCount=" + response.orphanObjectCount()
                + " missingObjectCount=" + response.missingObjectCount()
                + " failedOrIncompleteCount=" + response.failedOrIncompleteCount()
                + " purgeableDeletedCount=" + response.purgeableDeletedCount()
                + " failures=" + response.failures().size();
    }

    private static class CleanupCounters {
        private int orphanObjectsDeleted;
        private int materialsPurged;
    }
}
