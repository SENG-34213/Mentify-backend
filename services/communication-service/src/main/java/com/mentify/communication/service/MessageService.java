package com.mentify.communication.service;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.enums.MessageDeleteScope;

import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID groupId, SendMessageRequest request);

    MessageResponse sendMessage(UUID groupId, SendMessageRequest request, UUID senderId);

    PageResponse<MessageResponse> getMessageHistory(UUID groupId, int page, int size);

    MessageResponse updateMessage(UUID groupId, UUID messageId, UpdateMessageRequest request);

    void deleteMessage(UUID groupId, UUID messageId, MessageDeleteScope scope);
}
