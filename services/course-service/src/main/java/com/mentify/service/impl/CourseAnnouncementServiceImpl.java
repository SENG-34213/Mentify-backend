package com.mentify.service.impl;

import com.mentify.client.EntrollmentServiceClient;
import com.mentify.dto.AnnouncementCreateRequest;
import com.mentify.dto.AnnouncementResponse;
import com.mentify.entity.Course;
import com.mentify.entity.CourseAnnouncement;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.CourseAnnouncementRepository;
import com.mentify.repository.CourseRepository;
import com.mentify.service.CourseAnnouncementService;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseAnnouncementServiceImpl implements CourseAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final CourseContentAccessGuard courseContentAccessGuard;
    private final EntrollmentServiceClient enrollmentClient;

    @Override
    @Transactional
    public AnnouncementResponse create(UUID courseId, AnnouncementCreateRequest request, String authorizationHeader) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        courseContentAccessGuard.assertCanReadCourseContent(course, authorizationHeader);
        CourseAnnouncement saved = announcementRepository.save(CourseAnnouncement.builder()
                .courseId(courseId).authorId(currentUserId())
                .title(request.getTitle().trim()).message(request.getMessage().trim())
                .publishedAt(LocalDateTime.now()).build());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getMyAnnouncements(UUID courseId, int page, int size,
                                                        String authorizationHeader) {
        UUID studentId = currentUserId();
        ApiResponse<List<UUID>> enrollmentResponse;
        try {
            enrollmentResponse = enrollmentClient.getStudentCourseIds(studentId, authorizationHeader);
        } catch (FeignException exception) {
            throw new IllegalStateException("Enrollment service is unavailable", exception);
        }
        if (enrollmentResponse == null || enrollmentResponse.getData() == null) {
            throw new IllegalStateException("Enrollment service returned no course list");
        }
        List<UUID> courseIds = enrollmentResponse.getData().stream().distinct().toList();
        if (courseId != null && !courseIds.contains(courseId)) {
            throw new AccessDeniedException("Student is not enrolled in the requested course");
        }
        if (courseIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        Page<CourseAnnouncement> pageResult = courseId == null
                ? announcementRepository.findAllByCourseIdInAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
                        courseIds, LocalDateTime.now(), PageRequest.of(page, size))
                : announcementRepository.findAllByCourseIdInAndPublishedAtLessThanEqualAndCourseIdOrderByPublishedAtDesc(
                        courseIds, LocalDateTime.now(), courseId, PageRequest.of(page, size));
        return pageResult.map(this::toResponse);
    }

    private UUID currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Jwt jwt) || jwt.getSubject() == null) {
            throw new AccessDeniedException("Authenticated user identity is missing");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Authenticated user identity is invalid");
        }
    }

    private AnnouncementResponse toResponse(CourseAnnouncement announcement) {
        return AnnouncementResponse.builder().id(announcement.getId()).courseId(announcement.getCourseId())
                .authorId(announcement.getAuthorId()).title(announcement.getTitle())
                .message(announcement.getMessage()).publishedAt(announcement.getPublishedAt()).build();
    }
}
