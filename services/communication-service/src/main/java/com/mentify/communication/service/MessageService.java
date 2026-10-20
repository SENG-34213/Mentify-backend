package com.mentify.communication.service;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.dto.response.CommunicationAttachmentResponse;
import com.mentify.communication.dto.response.AttachmentDownloadResponse;
import org.springframework.web.multipart.MultipartFile;
import com.mentify.communication.enums.MessageDeleteScope;

import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID groupId, SendMessageRequest request);

    MessageResponse sendMessage(UUID groupId, SendMessageRequest request, UUID senderId);

    CommunicationAttachmentResponse uploadAttachment(UUID groupId, MultipartFile file);

    AttachmentDownloadResponse downloadAttachment(UUID groupId, UUID messageId, UUID attachmentId);

    PageResponse<MessageResponse> getMessageHistory(UUID groupId, int page, int size);

    MessageResponse updateMessage(UUID groupId, UUID messageId, UpdateMessageRequest request);

    void deleteMessage(UUID groupId, UUID messageId, MessageDeleteScope scope);
}
