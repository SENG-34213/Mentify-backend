package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherStudentDirectoryResponse {
    UUID id;
    String name;
    String email;
    String studentNumber;
    String avatar;
    String program;
    String status;
    List<UUID> courses;
}
