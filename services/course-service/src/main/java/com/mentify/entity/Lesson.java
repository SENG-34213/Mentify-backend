package com.mentify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "lessons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Estimated lesson length in whole minutes. */
    @Column(name = "duration_minutes", nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer durationMinutes = 1;

    /** Markdown/plain-text lesson body authored by the teacher. */
    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(name = "content_type", length = 30)
    private String contentType;

    @Column(nullable = false)
    private boolean isVisible = true;

    @Column(name = "teaching_completed", nullable = false)
    private boolean teachingCompleted = false;

    /**
     * Date on which this lesson becomes accessible to enrolled students.
     */
    private LocalDate releaseDate;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningMaterial> learningMaterials = new ArrayList<>();


}
