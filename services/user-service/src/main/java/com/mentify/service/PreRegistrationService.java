package com.mentify.service;

import com.mentify.dto.PreRegistrationRequest;
import com.mentify.dto.PreRegistrationResponse;
import com.mentify.dto.PreRegistrationUpdateRequest;
import com.mentify.entity.PreRegistration;
import com.mentify.enums.PreRegistrationStatus;
import com.mentify.repository.PreRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreRegistrationService {
    private final PreRegistrationRepository repository;

    @Transactional
    public PreRegistrationResponse create(PreRegistrationRequest request) {
        if (!Boolean.TRUE.equals(request.getConsent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent is required.");
        }

        PreRegistration registration = PreRegistration.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .role(request.getRole())
                .district(request.getDistrict())
                .city(request.getCity().trim())
                .classFormat(request.getClassFormat())
                .grade(request.getGrade())
                .subjects(request.getSubjects())
                .contactMethod(request.getContactMethod())
                .consent(true)
                .status(PreRegistrationStatus.NEW)
                .seen(false)
                .build();
        return toResponse(repository.save(registration));
    }

    @Transactional(readOnly = true)
    public Page<PreRegistrationResponse> findAll(
            PreRegistrationStatus status,
            Boolean seen,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByFilters(status, seen, pageable).map(this::toResponse);
    }

    @Transactional
    public PreRegistrationResponse update(UUID id, PreRegistrationUpdateRequest request) {
        PreRegistration registration = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pre-registration not found."));

        if (request.getStatus() != null)
            registration.setStatus(request.getStatus());
        if (request.getSeen() != null)
            registration.setSeen(request.getSeen());
        if (request.getAdminRemark() != null)
            registration.setAdminRemark(request.getAdminRemark().trim());
        return toResponse(repository.save(registration));
    }

    private PreRegistrationResponse toResponse(PreRegistration registration) {
        return new PreRegistrationResponse(
                registration.getId(),
                registration.getName(),
                registration.getEmail(),
                registration.getPhone(),
                registration.getRole(),
                registration.getDistrict(),
                registration.getCity(),
                registration.getClassFormat(),
                registration.getGrade(),
                registration.getSubjects(),
                registration.getContactMethod(),
                registration.isConsent(),
                registration.getStatus(),
                registration.isSeen(),
                registration.getAdminRemark(),
                registration.getCreatedAt(),
                registration.getUpdatedAt());
    }
}