package com.mentify.service.impl;

import com.mentify.dto.GradeMonthlyFeeRequest;
import com.mentify.dto.GradeMonthlyFeeResponse;
import com.mentify.entity.Grade;
import com.mentify.entity.GradeMonthlyFee;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.GradeMonthlyFeeRepository;
import com.mentify.repository.GradeRepository;
import com.mentify.service.GradeMonthlyFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeMonthlyFeeServiceImpl implements GradeMonthlyFeeService {

    private final GradeMonthlyFeeRepository feeRepository;
    private final GradeRepository gradeRepository;

    @Override
    public ApiResponse<List<GradeMonthlyFeeResponse>> getAll() {
        List<GradeMonthlyFeeResponse> fees = feeRepository.findAllByOrderByGrade_NameAsc().stream()
                .map(this::toResponse).toList();
        return ApiResponse.<List<GradeMonthlyFeeResponse>>builder()
                .statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message("Grade monthly fees fetched successfully").data(fees).build();
    }

    @Override
    public ApiResponse<GradeMonthlyFeeResponse> getByGrade(UUID gradeId) {
        return ok("Grade monthly fee fetched successfully", findByGrade(gradeId));
    }

    @Override
    @Transactional
    public ApiResponse<GradeMonthlyFeeResponse> create(UUID gradeId, GradeMonthlyFeeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", gradeId));
        if (feeRepository.existsByGrade_Id(gradeId)) {
            throw new ResourceAlreadyExistsException("Monthly fee already exists for grade " + grade.getName());
        }
        GradeMonthlyFee fee = GradeMonthlyFee.builder()
                .grade(grade).monthlyFee(request.getMonthlyFee()).currencyCode(normalizeCurrency(request.getCurrencyCode()))
                .build();
        try {
            return new ApiResponse<>(HttpStatus.CREATED.value(), "Grade monthly fee created successfully",
                    toResponse(feeRepository.saveAndFlush(fee)), HttpStatus.CREATED);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceAlreadyExistsException("Monthly fee already exists for grade " + grade.getName());
        }
    }

    @Override
    @Transactional
    public ApiResponse<GradeMonthlyFeeResponse> update(UUID gradeId, GradeMonthlyFeeRequest request) {
        GradeMonthlyFee fee = findByGrade(gradeId);
        fee.setMonthlyFee(request.getMonthlyFee());
        fee.setCurrencyCode(normalizeCurrency(request.getCurrencyCode()));
        return ok("Grade monthly fee updated successfully", toResponse(feeRepository.save(fee)));
    }

    @Override
    @Transactional
    public ApiResponse<Object> delete(UUID gradeId) {
        GradeMonthlyFee fee = findByGrade(gradeId);
        feeRepository.delete(fee);
        return ApiResponse.builder().statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message("Grade monthly fee deleted successfully").build();
    }

    private GradeMonthlyFee findByGrade(UUID gradeId) {
        return feeRepository.findByGrade_Id(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly fee", "grade id", gradeId));
    }

    private GradeMonthlyFeeResponse toResponse(GradeMonthlyFee fee) {
        return GradeMonthlyFeeResponse.builder().id(fee.getId()).gradeId(fee.getGrade().getId())
                .gradeName(fee.getGrade().getName()).monthlyFee(fee.getMonthlyFee())
                .currencyCode(fee.getCurrencyCode()).build();
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message(message).data(data).build();
    }

    private String normalizeCurrency(String currencyCode) {
        return currencyCode == null || currencyCode.isBlank() ? "LKR" : currencyCode.toUpperCase(Locale.ROOT);
    }
}
