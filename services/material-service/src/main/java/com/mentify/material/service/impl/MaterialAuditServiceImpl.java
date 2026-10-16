package com.mentify.material.service.impl;

import com.mentify.material.entity.Material;
import com.mentify.material.entity.MaterialAuditEvent;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.repository.MaterialAuditEventRepository;
import com.mentify.material.service.MaterialAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAuditServiceImpl implements MaterialAuditService {

    private final MaterialAuditEventRepository auditEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(MaterialAuditAction action, Material material, UUID actorId, String outcome, String details) {
        try {
            auditEventRepository.saveAndFlush(MaterialAuditEvent.builder()
                    .action(action)
                    .materialId(material != null ? material.getId() : null)
                    .actorId(actorId)
                    .courseId(material != null ? material.getCourseId() : null)
                    .outcome(safeOutcome(outcome))
                    .details(truncate(details))
                    .build());
            log.info(
                    "material_audit action={} materialId={} actorId={} courseId={} outcome={}",
                    action,
                    material != null ? material.getId() : null,
                    actorId,
                    material != null ? material.getCourseId() : null,
                    safeOutcome(outcome)
            );
        } catch (RuntimeException ex) {
            log.warn("Material audit event recording failed action={} outcome={}: {}", action, outcome, ex.getClass().getSimpleName());
        }
    }

    @Override
    public void recordCleanup(String outcome, String details) {
        record(MaterialAuditAction.CLEANUP, null, null, outcome, details);
    }

    private String safeOutcome(String outcome) {
        return outcome == null || outcome.isBlank() ? "SUCCESS" : outcome.trim();
    }

    private String truncate(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        String trimmed = details.trim();
        return trimmed.length() <= 1000 ? trimmed : trimmed.substring(0, 1000);
    }
}
