package com.mentify.ai.service.impl;

import com.mentify.ai.dto.request.AiConversationCreateRequest;
import com.mentify.ai.dto.request.AiConversationUpdateRequest;
import com.mentify.ai.dto.response.AiConversationDetailResponse;
import com.mentify.ai.dto.response.AiConversationResponse;
import com.mentify.ai.entity.AiConversation;
import com.mentify.ai.entity.AiMessage;
import com.mentify.ai.enums.AiMessageRole;
import com.mentify.ai.exception.AiConversationNotFoundException;
import com.mentify.ai.repository.AiConversationRepository;
import com.mentify.ai.repository.AiMessageRepository;
import com.mentify.ai.security.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConversationServiceImplTest {

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiMessageRepository messageRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private AiConversationServiceImpl conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new AiConversationServiceImpl(
                conversationRepository,
                messageRepository,
                authenticatedUserService
        );
    }

    @Test
    void createConversationUsesAuthenticatedUserAndClientTitleOnly() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(conversationRepository.save(org.mockito.ArgumentMatchers.any(AiConversation.class)))
                .thenAnswer(invocation -> {
                    AiConversation conversation = invocation.getArgument(0);
                    conversation.setId(conversationId);
                    return conversation;
                });

        AiConversationResponse response = conversationService.createConversation(
                AiConversationCreateRequest.builder().title("  Planning help  ").build()
        );

        ArgumentCaptor<AiConversation> conversationCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(conversationCaptor.getValue().getTitle()).isEqualTo("Planning help");
        assertThat(response.getId()).isEqualTo(conversationId);
        assertThat(response.getTitle()).isEqualTo("Planning help");
    }

    @Test
    void getCurrentUserConversationsLoadsOnlyAuthenticatedUserOrderedByRepository() {
        UUID userId = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title("Recent")
                .build();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(conversation));

        List<AiConversationResponse> response = conversationService.getCurrentUserConversations();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Recent");
        verify(conversationRepository).findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Test
    void getConversationLoadsOwnedConversationAndMessagesInAscendingCreationOrder() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title("Lesson summary")
                .build();
        conversation.setId(conversationId);
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessageRole.USER)
                .content("Summarize lesson")
                .build();
        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessageRole.ASSISTANT)
                .content("Summary")
                .build();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMessage, assistantMessage));

        AiConversationDetailResponse response = conversationService.getConversation(conversationId);

        assertThat(response.getTitle()).isEqualTo("Lesson summary");
        assertThat(response.getMessages())
                .extracting(message -> message.getRole())
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
    }

    @Test
    void updateConversationRejectsConversationNotOwnedByCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.updateConversation(
                conversationId,
                AiConversationUpdateRequest.builder().title("New title").build()
        )).isInstanceOf(AiConversationNotFoundException.class);
    }

    @Test
    void deleteConversationDeletesOnlyOwnedConversation() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title("Delete me")
                .build();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));

        conversationService.deleteConversation(conversationId);

        verify(conversationRepository).delete(conversation);
    }
}
