package com.mentify.ai.controller;

import com.mentify.ai.dto.request.AiConversationCreateRequest;
import com.mentify.ai.dto.request.AiConversationUpdateRequest;
import com.mentify.ai.dto.response.AiConversationDetailResponse;
import com.mentify.ai.dto.response.AiConversationResponse;
import com.mentify.ai.service.AiConversationService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class AiConversationController {

    private final AiConversationService conversationService;

    @PostMapping
    public ResponseEntity<ApiResponse<AiConversationResponse>> createConversation(
            @Valid @RequestBody(required = false) AiConversationCreateRequest request
    ) {
        AiConversationResponse response = conversationService.createConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "AI conversation created successfully",
                        response
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiConversationResponse>>> getConversations() {
        List<AiConversationResponse> response = conversationService.getCurrentUserConversations();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "AI conversations retrieved successfully",
                response
        ));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<AiConversationDetailResponse>> getConversation(
            @PathVariable UUID conversationId
    ) {
        AiConversationDetailResponse response = conversationService.getConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "AI conversation retrieved successfully",
                response
        ));
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<AiConversationResponse>> updateConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody AiConversationUpdateRequest request
    ) {
        AiConversationResponse response = conversationService.updateConversation(conversationId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "AI conversation updated successfully",
                response
        ));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable UUID conversationId) {
        conversationService.deleteConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "AI conversation deleted successfully",
                null
        ));
    }
}
