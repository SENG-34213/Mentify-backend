package com.mentify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {

    private UUID id;
    private String title;
    private Integer sequenceOrder;
    private String description;
    private UUID courseId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isVisible;
    private String moduleImage;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
