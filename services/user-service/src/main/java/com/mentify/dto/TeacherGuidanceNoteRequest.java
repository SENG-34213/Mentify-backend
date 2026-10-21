package com.mentify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherGuidanceNoteRequest {
    @NotBlank @Size(max = 4000)
    private String note;
}
