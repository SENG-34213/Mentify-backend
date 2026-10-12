package com.mentify.material.service;

import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;

import java.util.UUID;

public interface MaterialAuditService {

    void record(MaterialAuditAction action, Material material, UUID actorId, String outcome, String details);

    void recordCleanup(String outcome, String details);
}
