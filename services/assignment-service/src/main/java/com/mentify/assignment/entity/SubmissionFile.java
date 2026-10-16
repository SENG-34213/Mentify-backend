package com.mentify.assignment.entity;

import com.mentify.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "submission_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(length = 255)
    private String storedFilename;

    @Column(nullable = false, length = 255)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(columnDefinition = "uuid")
    private UUID materialId;

    public boolean isMaterialBacked() {
        return materialId != null;
    }
}
