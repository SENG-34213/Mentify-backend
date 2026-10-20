package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.mentify.enums.MaterialType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMaterial extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialType type;

    @Column(length = 500)
    private String fileUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 30)
    private String status;

    @Column(name = "material_id", columnDefinition = "uuid")
    private UUID materialId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;
}
