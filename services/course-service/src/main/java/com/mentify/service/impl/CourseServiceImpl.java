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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public ApiResponse<CourseResponse> createCourse(CourseRequest request) {

        log.info("Creating course with title '{}'", request.getCourseName());

        if (courseRepository.existsByCourseNameAndGradeId(request.getCourseName(), request.getGradeId())) {
            log.warn("Duplicate course title attempted: '{}'", request.getCourseName());
            throw new ResourceAlreadyExistsException("Course", "courseName", request.getCourseName());
        }

        // Verify the assigned Teacher exists in the local user-service DB


        Course course = CourseMapper.toCourseEntity(request);

        Course savedCourse = courseRepository.save(course);

        CourseResponse courseResponse = CourseMapper.toCourseResponse(savedCourse);
        log.info("Course created successfully with ID [{}]", savedCourse.getId());

        return ApiResponse.<CourseResponse>builder()
                .message("Course created successfully")
                .data(courseResponse)
                .status(HttpStatus.CREATED)
                .build();
    }
}
