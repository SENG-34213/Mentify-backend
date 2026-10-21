package com.mentify.communication.controller;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.dto.response.CommunicationAttachmentResponse;
import com.mentify.communication.dto.response.AttachmentDownloadResponse;
import com.mentify.communication.enums.MessageDeleteScope;
import com.mentify.communication.service.MessageService;
import com.mentify.communication.service.MessageBroadcastService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.UUID;

@RestController
@RequestMapping("/api/communication/groups/{groupId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageBroadcastService messageBroadcastService;

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommunicationAttachmentResponse>> uploadAttachment(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file
    ) {
        CommunicationAttachmentResponse response = messageService.uploadAttachment(groupId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Attachment uploaded successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID groupId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = messageService.sendMessage(groupId, request);
        messageBroadcastService.broadcast(groupId, response);

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

    @PatchMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateMessage(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        MessageResponse response = messageService.updateMessage(groupId, messageId, request);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Message updated successfully", response));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "ME") MessageDeleteScope scope
    ) {
        messageService.deleteMessage(groupId, messageId, scope);

        String message = MessageDeleteScope.EVERYONE.equals(scope)
                ? "Message deleted for everyone successfully"
                : "Message deleted for you successfully";

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), message, null));
    }

    @GetMapping("/{messageId}/attachments/{attachmentId}/download")
    public ResponseEntity<ApiResponse<AttachmentDownloadResponse>> downloadAttachment(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            @PathVariable UUID attachmentId
    ) {
        AttachmentDownloadResponse response = messageService.downloadAttachment(groupId, messageId, attachmentId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Attachment download URL generated", response));
    }
}
