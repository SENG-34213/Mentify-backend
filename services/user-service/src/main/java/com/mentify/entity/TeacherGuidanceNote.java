package com.mentify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "teacher_guidance_notes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherGuidanceNote extends BaseEntity {
    @Column(nullable = false, columnDefinition = "uuid")
    private UUID studentId;
    @Column(nullable = false, columnDefinition = "uuid")
    private UUID teacherId;
    @Column(nullable = false, length = 4000)
    private String note;
}
