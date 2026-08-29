package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.mentify.enums.MaterialType;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;
}
