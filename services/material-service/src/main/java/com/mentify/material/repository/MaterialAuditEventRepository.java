package com.mentify.material.repository;

import com.mentify.material.entity.MaterialAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialAuditEventRepository extends JpaRepository<MaterialAuditEvent, UUID> {
}
