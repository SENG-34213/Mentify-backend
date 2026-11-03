package com.mentify.dto;

import com.mentify.enums.PreRegistrationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreRegistrationResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String role,
        String district,
        String city,
        String classFormat,
        String grade,
        List<String> subjects,
        String contactMethod,
        boolean consent,
        PreRegistrationStatus status,
        boolean seen,
        String adminRemark,
        Instant createdAt,
        Instant updatedAt) {
}