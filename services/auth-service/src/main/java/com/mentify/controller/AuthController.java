package com.mentify.controller;

import com.mentify.config.AuthProperties;
import com.mentify.dto.AuthResponse;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.ProvisionUserRequest;
import com.mentify.dto.TokenRequest;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.AuthenticationService;
import com.mentify.service.JwtTokenService;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "Login successful", authenticationService.login(request)));
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
