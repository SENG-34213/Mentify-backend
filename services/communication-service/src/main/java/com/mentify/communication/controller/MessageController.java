package com.mentify.communication.controller;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.service.MessageService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/communication/groups/{groupId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID groupId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = messageService.sendMessage(groupId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Message sent successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessageHistory(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        PageResponse<MessageResponse> response = messageService.getMessageHistory(groupId, page, size);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Messages fetched successfully", response));
    }
}
