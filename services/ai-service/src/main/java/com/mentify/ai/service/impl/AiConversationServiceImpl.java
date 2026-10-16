package com.mentify.ai.service.impl;

import com.mentify.ai.dto.request.AiConversationCreateRequest;
import com.mentify.ai.dto.request.AiConversationUpdateRequest;
import com.mentify.ai.dto.response.AiConversationDetailResponse;
import com.mentify.ai.dto.response.AiConversationResponse;
import com.mentify.ai.dto.response.AiMessageResponse;
import com.mentify.ai.entity.AiConversation;
import com.mentify.ai.entity.AiMessage;
import com.mentify.ai.exception.AiConversationNotFoundException;
import com.mentify.ai.repository.AiConversationRepository;
import com.mentify.ai.repository.AiMessageRepository;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public AiConversationResponse createConversation(AiConversationCreateRequest request) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .build();

        if (request != null) {
            conversation.setTitle(request.getTitle());
        }

        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiConversationResponse> getCurrentUserConversations() {
        UUID userId = authenticatedUserService.getCurrentUserId();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiConversationDetailResponse getConversation(UUID conversationId) {
        AiConversation conversation = findOwnedConversation(conversationId);
        List<AiMessageResponse> messages = loadConversationMessages(conversationId);

        return AiConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageResponse> getConversationMessages(UUID conversationId) {
        findOwnedConversation(conversationId);
        return loadConversationMessages(conversationId);
    }

    @Override
    @Transactional
    public AiConversationResponse updateConversation(UUID conversationId, AiConversationUpdateRequest request) {
        AiConversation conversation = findOwnedConversation(conversationId);
        conversation.setTitle(request.getTitle());
        return toConversationResponse(conversationRepository.saveAndFlush(conversation));
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId) {
        AiConversation conversation = findOwnedConversation(conversationId);
        conversationRepository.delete(conversation);
    }

    private AiConversation findOwnedConversation(UUID conversationId) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(AiConversationNotFoundException::new);
    }

    private List<AiMessageResponse> loadConversationMessages(UUID conversationId) {
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private AiConversationResponse toConversationResponse(AiConversation conversation) {
        return AiConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private AiMessageResponse toMessageResponse(AiMessage message) {
        return AiMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
