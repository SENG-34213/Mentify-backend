package com.mentify.mapper;


import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import java.util.UUID;
import com.mentify.entity.Grade;


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
        return Course.builder()
                .courseName(request.getCourseName().trim())
                .courseDescription(request.getCourseDescription().trim())
                .courseThumbnail(request.getCourseThumbnail())
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .isOnline(isOnline)
                .isVisible(isVisible)
                .gradeId(request.getGradeId())
                .assignedTeacherId(request.getAssignedTeacherId())
                .isPublished(isPublished)
                .status(isPublished ? CourseStatus.PUBLISHED : CourseStatus.DRAFT)
                .numberOfStudents(0)
                .build();
    }

    /**
     * Maps a persisted Course entity to a CourseResponse DTO.
     */
    public static CourseResponse toCourseResponse(Course course) {
        return toCourseResponse(course, null);
    }

    public static CourseResponse toCourseResponse(Course course, Grade grade) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .courseThumbnail(course.getCourseThumbnail())
                .gradeMonthlyFee(grade != null ? grade.getMonthlyFee() : null)
                .gradeCurrencyCode(grade != null ? grade.getCurrencyCode() : null)
                .assignedTeacherId(course.getAssignedTeacherId())
                .gradeId(course.getGradeId())
                .subject(course.getSubject())
                .online(course.isOnline())
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
