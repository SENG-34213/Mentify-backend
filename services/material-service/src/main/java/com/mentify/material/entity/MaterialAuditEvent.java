package com.mentify.material.entity;

import com.mentify.entity.BaseEntity;
import com.mentify.material.enums.MaterialAuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "material_audit_events",
        indexes = {
                @Index(name = "idx_material_audit_material_id", columnList = "material_id"),
                @Index(name = "idx_material_audit_action_created", columnList = "action, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAuditEvent extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MaterialAuditAction action;

    @Column(name = "material_id", columnDefinition = "uuid")
    private UUID materialId;

    @Column(name = "actor_id", columnDefinition = "uuid")
    private UUID actorId;

    @Column(name = "course_id", columnDefinition = "uuid")
    private UUID courseId;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String outcome;

    @Size(max = 1000)
    @Column(length = 1000)
    private String details;
}
