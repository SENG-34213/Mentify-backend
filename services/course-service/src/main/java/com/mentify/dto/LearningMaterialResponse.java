package com.mentify.dto;

import com.mentify.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMaterialResponse {

    private UUID id;
    private String title;
    private MaterialType type;
    private String fileUrl;
    private UUID moduleId;
    private UUID lessonId;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
