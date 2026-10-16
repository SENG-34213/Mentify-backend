package com.mentify.dto;

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
public class GradeResponse {

    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private UUID createdBy;
    private UUID lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
