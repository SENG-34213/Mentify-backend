package com.mentify.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachAssignmentMaterialRequest {

    @NotNull(message = "Material ID is required")
    private UUID materialId;
}
