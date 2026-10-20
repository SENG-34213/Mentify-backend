package com.mentify.exam.controller;

import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.dto.response.StudentUpcomingExamResponse;
import com.mentify.exam.service.StudentExamService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    private final StudentExamService studentExamService;

    @GetMapping("/exams/me/upcoming")
    public ResponseEntity<ApiResponse<List<StudentUpcomingExamResponse>>> getUpcomingExams() {
        return ResponseEntity.ok(ApiResponse.success(200, "Upcoming exams fetched successfully",
                studentExamService.getUpcomingExams()));
    }

    @GetMapping("/exams/{examId}/student")
    public ResponseEntity<ApiResponse<ExamResponse>> getStudentExam(@PathVariable UUID examId) {
        return ResponseEntity.ok(ApiResponse.success(200, "Student exam fetched successfully",
                studentExamService.getStudentExam(examId)));
    }
}
