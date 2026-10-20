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
    String studentNumber;
    String studentName;
    String email;
    List<TeacherGradebookItemResponse> assignments;
    List<TeacherGradebookItemResponse> quizzes;
    List<TeacherGradebookItemResponse> exams;
    List<TeacherGradebookItemResponse> assignmentScores;
    List<TeacherGradebookItemResponse> quizScores;
    List<TeacherGradebookItemResponse> examScores;
    BigDecimal total;
    BigDecimal percentage;
    String grade;
    BigDecimal overallPercentage;
    String letterGrade;
    String status;
}
