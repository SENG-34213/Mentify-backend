package com.mentify.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalLessonResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID courseId;
}
