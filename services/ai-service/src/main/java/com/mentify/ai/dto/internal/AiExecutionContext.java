package com.mentify.ai.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecutionContext {
    private String tenantId;
    private UUID courseId;
    private UUID lessonId;
}
