package com.mentify.notification.service;

import com.mentify.exception.ResourceNotFoundException;
import com.mentify.notification.dto.AnnouncementRequest;
import com.mentify.notification.dto.AnnouncementResponse;
import com.mentify.notification.entity.Announcement;
import com.mentify.notification.entity.AnnouncementAudience;
import com.mentify.notification.entity.AnnouncementRead;
import com.mentify.notification.entity.AnnouncementStatus;
import com.mentify.notification.repository.AnnouncementReadRepository;
import com.mentify.notification.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getVisible(Authentication authentication, int page, int size) {
        String userId = userId(authentication);
        Page<Announcement> announcements = announcementRepository.findVisible(
                AnnouncementStatus.PUBLISHED,
                Instant.now(),
                audiencesFor(authentication),
                PageRequest.of(page, size)
        );
        return mapWithReadState(announcements, userId);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getVisibleById(UUID id, Authentication authentication) {
        String userId = userId(authentication);
        Announcement announcement = announcementRepository.findVisibleById(
                        id, AnnouncementStatus.PUBLISHED, Instant.now(), audiencesFor(authentication))
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        return toResponse(announcement, announcementReadRepository.existsByAnnouncementIdAndUserId(id, userId));
    }

    @Transactional
    public AnnouncementResponse markRead(UUID id, Authentication authentication) {
        String userId = userId(authentication);
        Announcement announcement = announcementRepository.findVisibleById(
                        id, AnnouncementStatus.PUBLISHED, Instant.now(), audiencesFor(authentication))
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        if (!announcementReadRepository.existsByAnnouncementIdAndUserId(id, userId)) {
            AnnouncementRead read = new AnnouncementRead();
            read.setId(UUID.randomUUID());
            read.setAnnouncementId(announcement.getId());
            read.setUserId(userId);
            read.setReadAt(Instant.now());
            announcementReadRepository.save(read);
        }
        return toResponse(announcement, true);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAdminAnnouncements(int page, int size, Authentication authentication) {
        String userId = userId(authentication);
        return mapWithReadState(announcementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)), userId);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getAdminAnnouncement(UUID id, Authentication authentication) {
        return toResponse(findAnnouncement(id), announcementReadRepository.existsByAnnouncementIdAndUserId(id, userId(authentication)));
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, Authentication authentication) {
        validateDates(request.publishAt(), request.expiresAt());
        Instant now = Instant.now();
        Announcement announcement = new Announcement();
        announcement.setId(UUID.randomUUID());
        announcement.setCreatedBy(userId(authentication));
        announcement.setCreatedAt(now);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        apply(request, announcement, now);
        return toResponse(announcementRepository.save(announcement), false);
    }

    @Transactional
    public AnnouncementResponse update(UUID id, AnnouncementRequest request, Authentication authentication) {
        validateDates(request.publishAt(), request.expiresAt());
        Announcement announcement = findAnnouncement(id);
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new IllegalStateException("Archived announcements cannot be updated");
        }
        apply(request, announcement, Instant.now());
        return toResponse(announcementRepository.save(announcement), false);
    }

    @Transactional
    public AnnouncementResponse publish(UUID id, Authentication authentication) {
        Announcement announcement = findAnnouncement(id);
        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new IllegalStateException("Archived announcements cannot be published");
        }
        Instant now = Instant.now();
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(now);
        if (announcement.getPublishAt() == null) {
            announcement.setPublishAt(now);
        }
        announcement.setUpdatedAt(now);
        return toResponse(announcementRepository.save(announcement), false);
    }

    @Transactional
    public AnnouncementResponse archive(UUID id, Authentication authentication) {
        Announcement announcement = findAnnouncement(id);
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcement.setUpdatedAt(Instant.now());
        return toResponse(announcementRepository.save(announcement), false);
    }

    @Transactional
    public void delete(UUID id) {
        Announcement announcement = findAnnouncement(id);
        announcementRepository.delete(announcement);
    }

    private Announcement findAnnouncement(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }

    private void apply(AnnouncementRequest request, Announcement announcement, Instant now) {
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setAudience(request.audience());
        announcement.setPriority(request.priority());
        announcement.setPublishAt(request.publishAt());
        announcement.setExpiresAt(request.expiresAt());
        announcement.setUpdatedAt(now);
    }

    private void validateDates(Instant publishAt, Instant expiresAt) {
        if (publishAt != null && expiresAt != null && !expiresAt.isAfter(publishAt)) {
            throw new IllegalArgumentException("expiresAt must be after publishAt");
        }
    }

    private Page<AnnouncementResponse> mapWithReadState(Page<Announcement> page, String userId) {
        Set<UUID> ids = page.getContent().stream().map(Announcement::getId).collect(Collectors.toSet());
        Set<UUID> readIds = ids.isEmpty()
                ? Set.of()
                : announcementReadRepository.findByAnnouncementIdInAndUserId(ids, userId).stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());
        return page.map(announcement -> toResponse(announcement, readIds.contains(announcement.getId())));
    }

    private AnnouncementResponse toResponse(Announcement announcement, boolean read) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAudience(),
                announcement.getPriority(),
                announcement.getStatus(),
                announcement.getPublishAt(),
                announcement.getExpiresAt(),
                announcement.getPublishedAt(),
                announcement.getCreatedBy(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                read
        );
    }

    private Collection<AnnouncementAudience> audiencesFor(Authentication authentication) {
        EnumSet<AnnouncementAudience> audiences = EnumSet.noneOf(AnnouncementAudience.class);
        if (authentication != null) {
            authentication.getAuthorities().forEach(authority -> addAudience(audiences, authority.getAuthority()));
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                addJwtRoles(audiences, jwt.getClaim("roles"));
                Object realmAccess = jwt.getClaims().get("realm_access");
                if (realmAccess instanceof java.util.Map<?, ?> access) {
                    addJwtRoles(audiences, access.get("roles"));
                }
            }
        }
        return audiences.isEmpty() ? Set.of(AnnouncementAudience.ALL) : audiences;
    }

    private void addJwtRoles(EnumSet<AnnouncementAudience> audiences, Object claim) {
        if (claim instanceof Collection<?> roles) {
            roles.forEach(role -> addAudience(audiences, String.valueOf(role)));
        } else if (claim != null) {
            addAudience(audiences, String.valueOf(claim));
        }
    }

    private void addAudience(EnumSet<AnnouncementAudience> audiences, String authority) {
        if (authority == null) return;
        String role = authority.trim().toUpperCase();
        if (role.startsWith("ROLE_")) role = role.substring(5);
        switch (role) {
            case "STUDENT" -> audiences.add(AnnouncementAudience.STUDENT);
            case "TEACHER" -> audiences.add(AnnouncementAudience.TEACHER);
            case "ADMIN", "SUPER_ADMIN" -> audiences.add(AnnouncementAudience.ADMIN);
            default -> { }
        }
    }

    private String userId(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return authentication.getName();
    }
}
