package com.mentify.service;

import com.mentify.dto.AnnouncementCreateRequest;
import com.mentify.dto.AnnouncementResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CourseAnnouncementService {
    AnnouncementResponse create(UUID courseId, AnnouncementCreateRequest request, String authorizationHeader);
    Page<AnnouncementResponse> getMyAnnouncements(UUID courseId, int page, int size, String authorizationHeader);
}
