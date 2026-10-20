package com.mentify.exam.service;

import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.dto.response.StudentUpcomingExamResponse;

import java.util.List;
import java.util.UUID;

public interface StudentExamService {
    List<StudentUpcomingExamResponse> getUpcomingExams();
    ExamResponse getStudentExam(UUID examId);
}
