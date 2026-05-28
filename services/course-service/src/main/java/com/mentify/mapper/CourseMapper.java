package com.mentify.mapper;


import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    /**
     * Maps a CourseRequest DTO to a new Course entity.
     * teacherId is intentionally not set here — it is injected
     * by the service layer from the authenticated principal.
     */
    public static Course toCourseEntity(CourseRequest request) {
        return Course.builder()
                .courseName(request.getCourseName())
                .courseDescription(request.getCourseDescription())
                .courseThumbnail(request.getCourseThumbnail())
                .courseFeeMonthly(request.getCourseFeeMonthly())
                .gradeId(request.getGradeId())
                .isPublished(false)
                .isVisible(true)
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
                .teacherId(course.getTeacherId())
                .gradeId(course.getGradeId())
                .isPublished(course.isPublished())
                .publishedDate(course.getPublishedDate())
                .isVisible(course.isVisible())
                .numberOfStudents(course.getNumberOfStudents())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}