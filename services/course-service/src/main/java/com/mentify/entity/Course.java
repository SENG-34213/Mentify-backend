package com.mentify.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String courseName;

    @Column(columnDefinition = "TEXT")
    private String courseDescription;

    @Column(length = 255)
    private String courseThumbnail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal courseFeeMonthly;

    /**
     * Reference to the teacher who owns this course (resolved via user-service).
     * Stored as a plain UUID — no @ManyToOne across service boundaries.
     */
    @Column(nullable = false, length = 36)
    private String teacherId;


    /**
     * Reference to the Grade this course belongs to (resolved via user-service).
     */
    @Column(nullable = false, length = 36)
    private String gradeId;

    @Column(nullable = false)
    private boolean isPublished = false;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private boolean isVisible = true;

    @Column(nullable = false)
    private int numberOfStudents = 0;


    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Module> modules = new ArrayList<>();





}
