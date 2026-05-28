package com.mentify.service.impl;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.mapper.CourseMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.CourseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public ApiResponse<CourseResponse> createCourse(CourseRequest request, String teacherId) {

        if (courseRepository.existsByCourseNameAndGradeId(request.getCourseName(), request.getGradeId())) {
            throw new ResourceAlreadyExistsException("Course", "courseName", request.getCourseName());
        }

        Course course = CourseMapper.toCourseEntity(request);
        course.setTeacherId(teacherId);

        Course savedCourse = courseRepository.save(course);

        CourseResponse courseResponse = CourseMapper.toCourseResponse(savedCourse);

        return ApiResponse.<CourseResponse>builder()
                .message("Course created successfully")
                .data(courseResponse)
                .status(HttpStatus.CREATED)
                .build();
    }
}
