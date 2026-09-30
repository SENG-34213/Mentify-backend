package com.mentify.ai.controller;

import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.service.AiChatService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Received AI chat request");
        AiChatResponse response = aiChatService.chat(request, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.<AiChatResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("AI chat response generated successfully")
                .data(response)
                .build());
    }
}
