package com.mentify.assignment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricItemResponse {
    private UUID id;
    private String criterion;
    private Integer marks;
    private Integer displayOrder;
}
