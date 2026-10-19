package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookResponse {
    UUID courseId;
    Map<String, Integer> gradingScale;
    List<TeacherGradebookStudentResponse> students;
}
