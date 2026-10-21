package com.mentify.service;

import com.mentify.entity.AuthUser;
import com.mentify.entity.PasswordResetToken;
import com.mentify.repository.AuthUserRepository;
import com.mentify.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int OTP_MINIMUM = 100_000;
    private static final int OTP_RANGE = 900_000;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthUserRepository authUsers;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordResetEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${mentify.password-reset.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Transactional
    public void request(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        AuthUser user = authUsers.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            return;
        }

        resetTokens.invalidateActiveTokens(user.getUserId());
        String otp = String.valueOf(OTP_MINIMUM + secureRandom.nextInt(OTP_RANGE));
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getUserId());
        token.setOtpHash(hash(otp));
        token.setExpiresAt(now().plusMinutes(Math.max(1, otpExpiryMinutes)));
        token.setCreatedAt(now());
        resetTokens.save(token);
        emailService.send(user.getEmail(), otp, Math.max(1, otpExpiryMinutes));
    }

    @Transactional
    public String verify(String rawEmail, String otp) {
        AuthUser user = authUsers.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));
        PasswordResetToken token = resetTokens.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));
        if (token.getExpiresAt().isBefore(now()) || token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS
                || !MessageDigest.isEqual(hash(otp).getBytes(StandardCharsets.UTF_8),
                token.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            resetTokens.save(token);
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        String resetToken = randomToken();
        token.setResetTokenHash(hash(resetToken));
        resetTokens.save(token);
        return resetToken;
    }

    @Transactional
    public void reset(String resetToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByResetTokenHashAndUsedFalse(hash(resetToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        if (token.getExpiresAt().isBefore(now()) || token.getResetTokenHash() == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        AuthUser user = authUsers.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setUpdatedAt(now());
        authUsers.save(user);
        token.setUsed(true);
        resetTokens.save(token);
        resetTokens.invalidateActiveTokens(user.getUserId());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String randomToken() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Secure hashing is unavailable", exception);
        }
    }
}
