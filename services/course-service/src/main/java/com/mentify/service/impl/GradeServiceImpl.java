package com.mentify.service.impl;

import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.entity.Grade;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.mapper.GradeMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.GradeRepository;
import com.mentify.service.GradeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public ApiResponse<GradeResponse> createGrade(GradeRequest request) {
        String gradeName = request.getName().trim();
        log.info("Creating grade '{}'", gradeName);

        if (gradeRepository.existsByNameIgnoreCase(gradeName)) {
            throw new ResourceAlreadyExistsException("Grade", "name", gradeName);
        }

        Grade savedGrade = gradeRepository.save(GradeMapper.toGradeEntity(request));

        return ApiResponse.<GradeResponse>builder()
                .message("Grade created successfully")
                .data(GradeMapper.toGradeResponse(savedGrade))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<GradeResponse> updateGrade(UUID gradeId, GradeRequest request) {
        String gradeName = request.getName().trim();
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", gradeId));

        if (gradeRepository.existsByNameIgnoreCaseAndIdNot(gradeName, gradeId)) {
            throw new ResourceAlreadyExistsException("Grade", "name", gradeName);
        }

        grade.setName(gradeName);
        grade.setDescription(GradeMapper.trimToNull(request.getDescription()));

        Grade savedGrade = gradeRepository.save(grade);

        return ApiResponse.<GradeResponse>builder()
                .message("Grade updated successfully")
                .data(GradeMapper.toGradeResponse(savedGrade))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<GradeResponse> getGradeById(UUID gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", gradeId));

        return ApiResponse.<GradeResponse>builder()
                .message("Grade fetched successfully")
                .data(GradeMapper.toGradeResponse(grade))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<GradeResponse>> getAllGrades() {
        List<GradeResponse> grades = gradeRepository.findAll().stream()
                .map(GradeMapper::toGradeResponse)
                .toList();

        return ApiResponse.<List<GradeResponse>>builder()
                .message("Grades fetched successfully")
                .data(grades)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteGrade(UUID gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", gradeId));

        if (courseRepository.existsByGradeId(gradeId)) {
            throw new ResourceAlreadyExistsException("Grade cannot be deleted because it is assigned to one or more courses");
        }

        gradeRepository.delete(grade);

        return ApiResponse.builder()
                .message("Grade deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }
}
