package com.mentify.communication.controller;

import com.mentify.communication.dto.AuthenticatedUserResponse;
import com.mentify.communication.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
public class CommunicationTestController {

    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/me")
    public AuthenticatedUserResponse me() {
        return new AuthenticatedUserResponse(authenticatedUserService.getCurrentUserId());
    }
}
