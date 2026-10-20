package com.mentify.exam.service;

import com.mentify.exam.dto.request.CreateExamRequest;
import com.mentify.exam.dto.request.UpdateExamRequest;
import com.mentify.exam.dto.response.ExamResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ExamService {

    ExamResponse createExam(CreateExamRequest request, String authorizationHeader);

    ExamResponse createExam(CreateExamRequest request, MultipartFile instructionFile,
                            boolean studentVisible, String authorizationHeader);

    ExamResponse getExam(UUID examId, String authorizationHeader);

    List<ExamResponse> getExamsByCourse(UUID courseId, String authorizationHeader);

    ExamResponse updateExam(UUID examId, UpdateExamRequest request, String authorizationHeader);

    ExamResponse updateExam(UUID examId, UpdateExamRequest request, MultipartFile instructionFile,
                            boolean studentVisible, String authorizationHeader);

    ExamResponse cancelExam(UUID examId, String authorizationHeader);

    ExamResponse scheduleExam(UUID examId, String authorizationHeader);

    ExamResponse startMarking(UUID examId, String authorizationHeader);

    ExamResponse completeExam(UUID examId, String authorizationHeader);
}
