package com.mentify.repository;

import com.mentify.entity.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {
    Page<CourseAnnouncement> findAllByCourseIdInAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
            List<UUID> courseIds, java.time.LocalDateTime now, Pageable pageable);
    Page<CourseAnnouncement> findAllByCourseIdInAndPublishedAtLessThanEqualAndCourseIdOrderByPublishedAtDesc(
            List<UUID> courseIds, java.time.LocalDateTime now, UUID courseId, Pageable pageable);
}
