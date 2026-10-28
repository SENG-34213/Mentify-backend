package com.mentify.notification.repository;

import com.mentify.notification.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, String userId);

    List<AnnouncementRead> findByAnnouncementIdInAndUserId(Collection<UUID> announcementIds, String userId);
}
