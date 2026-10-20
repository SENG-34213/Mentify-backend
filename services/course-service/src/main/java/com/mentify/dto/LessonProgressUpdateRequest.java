package com.mentify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonProgressUpdateRequest {
    @NotNull
    private Boolean completed;
}
