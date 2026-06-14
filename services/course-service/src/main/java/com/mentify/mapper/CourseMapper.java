package com.mentify.mapper;


import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;


public class CourseMapper {

    /**
     * Maps a CourseRequest DTO to a new Course entity.
     * teacherId is intentionally not set here — it is injected
     * by the service layer from the authenticated principal.
     */
    public static Course toCourseEntity(CourseRequest request) {
        return Course.builder()
                .courseName(request.getCourseName().trim())
                .courseDescription(request.getCourseDescription().trim())
                .courseThumbnail(request.getCourseThumbnail())
                .courseFeeMonthly(request.getCourseFeeMonthly())
                .gradeId(request.getGradeId())
                .isPublished(false)
                .assignedTeacherId(request.getAssignedTeacherId())
                .status(CourseStatus.DRAFT)
                .numberOfStudents(0)
                .build();
    }

    /**
     * Maps a persisted Course entity to a CourseResponse DTO.
     */
    public static CourseResponse toCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .courseThumbnail(course.getCourseThumbnail())
                .courseFeeMonthly(course.getCourseFeeMonthly())
                .assignedTeacherId(course.getAssignedTeacherId())
                .gradeId(course.getGradeId())
                .isPublished(course.isPublished())
                .courseStatus(course.getStatus())
                .publishedDate(course.getPublishedDate())
                .createdBy(course.getCreatedBy())
                .lastModifiedBy(course.getUpdatedBy())
                .numberOfStudents(course.getNumberOfStudents())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
