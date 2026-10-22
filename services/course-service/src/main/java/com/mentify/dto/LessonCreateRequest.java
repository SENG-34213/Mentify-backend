package com.mentify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonCreateRequest {

    @NotBlank(message = "Lesson title is required")
    @Size(max = 150, message = "Lesson title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Lesson duration is required")
    @Min(value = 1, message = "Lesson duration must be at least 1 minute")
    private Integer durationMinutes;

    @Size(max = 500000, message = "Lesson content must not exceed 500000 characters")
    private String contentMarkdown;

    @Size(max = 30, message = "Content type must not exceed 30 characters")
    private String contentType;

    private Boolean isVisible;

    private Boolean teachingCompleted;

    private LocalDate releaseDate;
}
