package com.mentify.entity;

import com.mentify.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;


    @Column(length = 150)
    private String subject;

    @Column(nullable = false)
    @Builder.Default
    private boolean isOnline = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVisible = true;

    /**
        * Reference to the teacher who owns this course.
        * Stores the auth user UUID (token sub) as a plain UUID value.
     */
    @Column(nullable = false, length = 36)
    private UUID assignedTeacherId;

    /**
     * Optional course enrollment reference used by downstream integrations.
     */
    /**
     * Reference to the Grade this course belongs to (resolved via user-service).
     */
    @Column(nullable = false, length = 36)
    private UUID gradeId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublished = false;

    private LocalDate publishedDate;

    @Column(nullable = false)
    private int numberOfStudents = 0;


    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

}
