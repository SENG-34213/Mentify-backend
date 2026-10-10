package com.mentify.exam.mapper;

import com.mentify.exam.dto.request.CreateExamRequest;
import com.mentify.exam.dto.request.UpdateExamRequest;
import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ExamMapper {

    public Exam toEntity(CreateExamRequest request, UUID createdBy) {
        return Exam.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalMarks(request.getTotalMarks())
                .passMarks(request.getPassMarks())
                .type(request.getType())
                .status(ExamStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    public void applyUpdate(Exam exam, UpdateExamRequest request) {
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setExamDate(request.getExamDate());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setPassMarks(request.getPassMarks());
        exam.setType(request.getType());
    }

    public ExamResponse toResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .courseId(exam.getCourseId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .examDate(exam.getExamDate())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .totalMarks(exam.getTotalMarks())
                .passMarks(exam.getPassMarks())
                .type(exam.getType())
                .status(exam.getStatus())
                .createdBy(exam.getCreatedBy())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }
}
