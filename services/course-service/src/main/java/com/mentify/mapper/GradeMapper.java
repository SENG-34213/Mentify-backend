package com.mentify.mapper;

import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.entity.Grade;

public class GradeMapper {

    private GradeMapper() {
    }

    public static Grade toGradeEntity(GradeRequest request) {
        return Grade.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .build();
    }

    public static GradeResponse toGradeResponse(Grade grade) {
        return GradeResponse.builder()
                .id(grade.getId())
                .name(grade.getName())
                .description(grade.getDescription())
                .active(grade.isActive())
                .createdBy(grade.getCreatedBy())
                .lastModifiedBy(grade.getUpdatedBy())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
