package com.mentify.material.entity;

import com.mentify.entity.BaseEntity;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "materials",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_materials_storage_key", columnNames = "storage_key")
        },
        indexes = {
                @Index(name = "idx_materials_uploaded_by", columnList = "uploaded_by"),
                @Index(name = "idx_materials_status", columnList = "status"),
                @Index(name = "idx_materials_type", columnList = "material_type"),
                @Index(name = "idx_materials_purpose", columnList = "material_purpose"),
                @Index(name = "idx_materials_course_id", columnList = "course_id"),
                @Index(name = "idx_materials_checksum", columnList = "checksum"),
                @Index(name = "idx_materials_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank
    @Size(max = 1024)
    @Pattern(regexp = "^(?!https?://).+", message = "S3 storage key must not be a public URL")
    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @NotBlank
    @Size(max = 255)
    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @NotNull
    @Positive
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 30)
    private MaterialType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "material_purpose", nullable = false, length = 40)
    private MaterialPurpose purpose;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MaterialStatus status = MaterialStatus.UPLOADED;

    @NotNull
    @Column(name = "uploaded_by", nullable = false, columnDefinition = "uuid")
    private UUID uploadedBy;

    @Column(name = "course_id", columnDefinition = "uuid")
    private UUID courseId;

    @Size(max = 128)
    @Column(length = 128)
    private String checksum;
}
