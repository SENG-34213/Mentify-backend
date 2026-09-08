package com.mentify.communication.service;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;

import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID groupId, SendMessageRequest request);

    PageResponse<MessageResponse> getMessageHistory(UUID groupId, int page, int size);
}
