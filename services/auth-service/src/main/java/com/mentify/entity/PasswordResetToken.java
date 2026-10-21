package com.mentify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String otpHash;

    @Column(length = 64)
    private String resetTokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column(nullable = false)
    private boolean used = false;
}
