package com.mentify.service.impl;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.mapper.CourseMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.CourseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        if (course.isPublished()) {
            course.setPublishedDate(LocalDate.now());
        } else {
            course.setPublishedDate(null);
        }

        Course savedCourse = courseRepository.save(course);

        CourseResponse courseResponse = CourseMapper.toCourseResponse(savedCourse);
        log.info("Course created successfully with ID [{}]", savedCourse.getId());

        return ApiResponse.<CourseResponse>builder()
                .message("Course created successfully")
                .data(courseResponse)
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> updateCourse(UUID courseId, CourseRequest request) {
        log.info("Updating course with ID [{}]", courseId);

        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (courseRepository.existsByCourseNameAndGradeIdAndIdNot(request.getCourseName(), request.getGradeId(), courseId)) {
            log.warn("Duplicate course title attempted on update: '{}'", request.getCourseName());
            throw new ResourceAlreadyExistsException("Course", "courseName", request.getCourseName());
        }

        existingCourse.setCourseName(request.getCourseName().trim());
        existingCourse.setCourseDescription(request.getCourseDescription().trim());
        existingCourse.setCourseThumbnail(request.getCourseThumbnail());
        existingCourse.setCourseFeeMonthly(request.getCourseFeeMonthly());
        existingCourse.setGradeId(request.getGradeId());
        existingCourse.setAssignedTeacherId(request.getAssignedTeacherId());
        existingCourse.setCourseEnrollmentId(request.getCourseEnrollmentId());
        existingCourse.setSubject(request.getSubject() != null ? request.getSubject().trim() : null);
        existingCourse.setOnline(request.getIsOnline() != null ? request.getIsOnline() : true);

        BigDecimal discountOfferPercent = request.getDiscountOfferPercent() != null
                ? request.getDiscountOfferPercent()
                : new BigDecimal("20.00");
        existingCourse.setDiscountOfferPercent(discountOfferPercent);
        existingCourse.setVisible(request.getIsVisible() != null ? request.getIsVisible() : true);

        boolean isPublished = request.getIsPublished() != null ? request.getIsPublished() : false;
        existingCourse.setPublished(isPublished);
        if (isPublished) {
            existingCourse.setStatus(com.mentify.enums.CourseStatus.PUBLISHED);
            if (existingCourse.getPublishedDate() == null) {
                existingCourse.setPublishedDate(LocalDate.now());
            }
        } else {
            existingCourse.setStatus(com.mentify.enums.CourseStatus.DRAFT);
            existingCourse.setPublishedDate(null);
        }

        Course savedCourse = courseRepository.save(existingCourse);

        CourseResponse courseResponse = CourseMapper.toCourseResponse(savedCourse);
        log.info("Course updated successfully with ID [{}]", savedCourse.getId());

        return ApiResponse.<CourseResponse>builder()
                .message("Course updated successfully")
                .data(courseResponse)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }
}
