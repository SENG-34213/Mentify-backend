package com.mentify.ai.controller;

import com.mentify.ai.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiUserController {

    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo() {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Set<String> roles = authenticatedUserService.getCurrentUserRoles();

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "roles", roles
        ));
    }
}
