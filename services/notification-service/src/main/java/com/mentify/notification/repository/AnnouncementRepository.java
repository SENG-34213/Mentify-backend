package com.mentify.notification.repository;

import com.mentify.notification.entity.Announcement;
import com.mentify.notification.entity.AnnouncementAudience;
import com.mentify.notification.entity.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("""
            select announcement from Announcement announcement
            where announcement.status = :status
              and (announcement.publishAt is null or announcement.publishAt <= :now)
              and (announcement.expiresAt is null or announcement.expiresAt > :now)
              and (announcement.audience = com.mentify.notification.entity.AnnouncementAudience.ALL
                   or announcement.audience in :audiences)
            order by announcement.priority desc, announcement.publishedAt desc, announcement.createdAt desc
            """)
    Page<Announcement> findVisible(
            @Param("status") AnnouncementStatus status,
            @Param("now") Instant now,
            @Param("audiences") java.util.Collection<AnnouncementAudience> audiences,
            Pageable pageable
    );

    @Query("""
            select announcement from Announcement announcement
            where announcement.id = :id
              and announcement.status = :status
              and (announcement.publishAt is null or announcement.publishAt <= :now)
              and (announcement.expiresAt is null or announcement.expiresAt > :now)
              and (announcement.audience = com.mentify.notification.entity.AnnouncementAudience.ALL
                   or announcement.audience in :audiences)
            """)
    Optional<Announcement> findVisibleById(
            @Param("id") UUID id,
            @Param("status") AnnouncementStatus status,
            @Param("now") Instant now,
            @Param("audiences") java.util.Collection<AnnouncementAudience> audiences
    );

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
