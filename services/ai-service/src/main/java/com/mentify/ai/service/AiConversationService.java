package com.mentify.ai.service;

import com.mentify.ai.dto.request.AiConversationCreateRequest;
import com.mentify.ai.dto.request.AiConversationUpdateRequest;
import com.mentify.ai.dto.response.AiConversationDetailResponse;
import com.mentify.ai.dto.response.AiConversationResponse;

import java.util.List;
import java.util.UUID;

public interface AiConversationService {

    AiConversationResponse createConversation(AiConversationCreateRequest request);

    List<AiConversationResponse> getCurrentUserConversations();

    AiConversationDetailResponse getConversation(UUID conversationId);

    AiConversationResponse updateConversation(UUID conversationId, AiConversationUpdateRequest request);

    void deleteConversation(UUID conversationId);
}
