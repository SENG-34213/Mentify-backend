package com.mentify.controller;

import com.mentify.config.AuthProperties;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalAuthProfileController {
    private final UserRepository users;
    private final AuthProperties properties;

    @GetMapping("/auth-profile")
    public ResponseEntity<AuthProfile> profile(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String email
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        return users.findByEmailIgnoreCase(email)
                .map(user -> ResponseEntity.ok(AuthProfile.from(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/auth-profile/activate")
    public ResponseEntity<AuthProfile> activate(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String email
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        return users.findByEmailIgnoreCase(email)
                .map(user -> {
                    if (user.getAccountStatus() == AccountStatus.INVITED) {
                        user.setAccountStatus(AccountStatus.ACTIVE);
                        user.setEmailVerified(true);
                        user.setAccountNonLocked(true);
                        user.setActive(true);
                        user = users.save(user);
                    }
                    return ResponseEntity.ok(AuthProfile.from(user));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/auth-profile/last-login")
    public ResponseEntity<Void> recordLastLogin(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String email
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        return users.findByEmailIgnoreCase(email)
                .map(user -> {
                    user.setLastLogin(LocalDateTime.now(ZoneOffset.UTC));
                    users.save(user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record AuthProfile(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            String role,
            String accountStatus,
            boolean accountNonLocked,
            boolean emailVerified,
            boolean profileComplete,
            boolean active
    ) {
        static AuthProfile from(User user) {
            boolean complete = switch (user.getRole()) {
                case STUDENT -> user.getStudentProfile() != null;
                case TEACHER -> user.getTeacherProfile() != null;
                case ADMIN, SUPER_ADMIN -> user.getAdminProfile() != null;
            };
            return new AuthProfile(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name(),
                    user.getAccountStatus().name(),
                    user.isAccountNonLocked(),
                    user.isEmailVerified(),
                    complete,
                    user.isActive()
            );
        }
    }
}
