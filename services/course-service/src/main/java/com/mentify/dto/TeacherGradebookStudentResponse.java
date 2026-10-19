package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookStudentResponse {
    UUID studentId;
    String studentName;
    List<TeacherGradebookItemResponse> assignments;
    List<TeacherGradebookItemResponse> quizzes;
    List<TeacherGradebookItemResponse> exams;
    BigDecimal total;
    BigDecimal percentage;
    String grade;
}
