package com.mentify.ai.repository;

import com.mentify.ai.entity.AiAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiAuditEventRepository extends JpaRepository<AiAuditEvent, UUID> {
}
