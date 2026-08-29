package com.mentify.mapper;


import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;


public class CourseMapper {

    /**
     * Maps a CourseRequest DTO to a new Course entity.
     * teacherId is intentionally not set here — it is injected
     * by the service layer from the authenticated principal.
     */
    public static Course toCourseEntity(CourseRequest request) {
        boolean isOnline = request.getIsOnline() != null ? request.getIsOnline() : true;
        boolean isVisible = request.getIsVisible() != null ? request.getIsVisible() : true;
        boolean isPublished = request.getIsPublished() != null ? request.getIsPublished() : false;
        BigDecimal discountOfferPercent = request.getDiscountOfferPercent() != null
                ? request.getDiscountOfferPercent()
                : new BigDecimal("20.00");

        return Course.builder()
                .courseName(request.getCourseName().trim())
                .courseDescription(request.getCourseDescription().trim())
                .courseThumbnail(request.getCourseThumbnail())
                .courseFeeMonthly(request.getCourseFeeMonthly())
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .isOnline(isOnline)
                .discountOfferPercent(discountOfferPercent)
                .isVisible(isVisible)
                .gradeId(request.getGradeId())
                .assignedTeacherId(request.getAssignedTeacherId())
                .courseEnrollmentId(request.getCourseEnrollmentId())
                .isPublished(isPublished)
                .status(isPublished ? CourseStatus.PUBLISHED : CourseStatus.DRAFT)
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
                .courseEnrollmentId(course.getCourseEnrollmentId())
                .gradeId(course.getGradeId())
                .subject(course.getSubject())
                .online(course.isOnline())
                .discountOfferPercent(course.getDiscountOfferPercent())
                .visible(course.isVisible())
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
