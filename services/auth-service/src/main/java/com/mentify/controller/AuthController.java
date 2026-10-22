package com.mentify.controller;

import com.mentify.config.AuthProperties;
import com.mentify.dto.AuthResponse;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.ProvisionUserRequest;
import com.mentify.dto.ForgotPasswordRequest;
import com.mentify.dto.ResetPasswordRequest;
import com.mentify.dto.VerifyResetOtpRequest;
import com.mentify.dto.TokenRequest;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.AuthenticationService;
import com.mentify.service.JwtTokenService;
import com.mentify.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final JwtTokenService tokenService;
    private final AuthProperties properties;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "Login successful", authenticationService.login(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.request(request.email());
        return ResponseEntity.ok(ApiResponse.success(200,
                "If an account exists for this email, a password reset code has been sent.", null));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        String resetToken = passwordResetService.verify(request.email(), request.otp());
        return ResponseEntity.ok(ApiResponse.success(200, "OTP verified successfully",
                Map.of("resetToken", resetToken)));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.resetToken(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(200, "Password reset successfully", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "Token refreshed successfully", authenticationService.refresh(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody TokenRequest request) {
        tokenService.revoke(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(200, "Logout successful", null));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return tokenService.jwks();
    }

    @PostMapping("/internal/users")
    public ResponseEntity<Void> provision(
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody ProvisionUserRequest request
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        authenticationService.provision(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/users/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable UUID userId,
            @RequestBody PasswordChangeRequest request
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        authenticationService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}

    @DeleteMapping("/internal/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable UUID userId
    ) {
        if (!properties.getInternalToken().equals(token)) {
            return ResponseEntity.status(403).build();
        }
        authenticationService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
