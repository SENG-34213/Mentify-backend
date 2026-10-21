package com.mentify.repository;

import com.mentify.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    Optional<PasswordResetToken> findByResetTokenHashAndUsedFalse(String resetTokenHash);

    @Modifying
    @Query("update PasswordResetToken token set token.used = true where token.userId = :userId and token.used = false")
    int invalidateActiveTokens(UUID userId);
}
