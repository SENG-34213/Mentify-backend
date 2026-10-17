package com.mentify.exam.dto.response;

import com.mentify.exam.enums.ExamStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookResponse {
    UUID courseId;
    List<GradebookExamResponse> exams;
    List<GradebookStudentResponse> students;

    @Value
    @Builder
    public static class GradebookExamResponse {
        UUID examId;
        String title;
        LocalDate examDate;
        ExamStatus status;
        BigDecimal totalMarks;
        BigDecimal passMarks;
    }

    @Value
    @Builder
    public static class GradebookStudentResponse {
        UUID studentId;
        String studentName;
        String studentNumber;
        String email;
        List<GradebookMarkResponse> results;
    }

    @Value
    @Builder
    public static class GradebookMarkResponse {
        UUID examId;
        BigDecimal marksObtained;
        String attendanceStatus;
        String resultStatus;
        String grade;
        String remarks;
    }
}
