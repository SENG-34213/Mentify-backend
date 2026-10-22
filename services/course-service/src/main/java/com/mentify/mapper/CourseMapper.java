package com.mentify.mapper;


import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import java.util.UUID;
import com.mentify.entity.Grade;


public class CourseMapper {

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

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
                .courseCode(trimToNull(request.getCourseCode()))
                .courseDescription(request.getCourseDescription().trim())
                .courseThumbnail(request.getCourseThumbnail())
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .level(trimToNull(request.getLevel()))
                .semester(trimToNull(request.getSemester()))
                .academicYear(trimToNull(request.getAcademicYear()))
                .maxCapacity(request.getMaxCapacity())
                .credits(request.getCredits())
                .schedule(trimToNull(request.getSchedule()))
                .gradingWeights(request.getGradingWeights() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(request.getGradingWeights()))
                .teachingAssistantIds(request.getTeachingAssistantIds() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(request.getTeachingAssistantIds()))
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
                .courseCode(course.getCourseCode())
                .courseDescription(course.getCourseDescription())
                .courseThumbnail(course.getCourseThumbnail())
                .gradeMonthlyFee(grade != null ? grade.getMonthlyFee() : null)
                .gradeCurrencyCode(grade != null ? grade.getCurrencyCode() : null)
                .gradeName(grade != null ? grade.getName() : null)
                .assignedTeacherId(course.getAssignedTeacherId())
                .gradeId(course.getGradeId())
                .subject(course.getSubject())
                .level(course.getLevel())
                .semester(course.getSemester())
                .academicYear(course.getAcademicYear())
                .online(course.isOnline())
                .visible(course.isVisible())
                .isPublished(course.isPublished())
                .courseStatus(course.getStatus())
                .publishedDate(course.getPublishedDate())
                .createdBy(course.getCreatedBy())
                .lastModifiedBy(course.getUpdatedBy())
                .numberOfStudents(course.getNumberOfStudents())
                .maxCapacity(course.getMaxCapacity())
                .credits(course.getCredits())
                .schedule(course.getSchedule())
                .gradingWeights(course.getGradingWeights())
                .teachingAssistantIds(course.getTeachingAssistantIds())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
