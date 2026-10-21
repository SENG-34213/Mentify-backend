package com.mentify.dto;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.UUID;

@Value @Builder
public class TeacherGuidanceNoteResponse {
    UUID id;
    UUID studentId;
    UUID teacherId;
    String teacherName;
    String teacherEmail;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
