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
@Table(name = "auth_refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
