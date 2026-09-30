package com.mentify.ai.websocket;

import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.dto.response.AiChatWebSocketResponse;
import com.mentify.ai.service.AiChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatWebSocketControllerTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private AiChatService aiChatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private AiChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new AiChatWebSocketController(aiChatService, messagingTemplate);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void websocketChatSendsProcessingAndCompletionResponses() {
        UUID conversationId = UUID.randomUUID();
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(conversationId)
                .message("What is inheritance?")
                .build();
        AiChatResponse chatResponse = AiChatResponse.builder()
                .message("Inheritance allows a class to reuse behavior from another class.")
                .toolsUsed(List.of())
                .build();
        Authentication authentication = authentication("admin-user", "ROLE_ADMIN");
        when(aiChatService.chat(request, AUTHORIZATION_HEADER)).thenReturn(chatResponse);

        controller.chat(request, AUTHORIZATION_HEADER, authentication, headerAccessor());

        ArgumentCaptor<AiChatWebSocketResponse> responseCaptor = ArgumentCaptor.forClass(AiChatWebSocketResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq("admin-user"),
                eq("/queue/ai/chat"),
                responseCaptor.capture()
        );
        assertThat(responseCaptor.getAllValues())
                .extracting(AiChatWebSocketResponse::getStatus)
                .containsExactly("PROCESSING", "COMPLETED");
        assertThat(responseCaptor.getAllValues().get(1).getConversationId()).isEqualTo(conversationId);
        assertThat(responseCaptor.getAllValues().get(1).getData()).isEqualTo(chatResponse);
        verify(aiChatService).chat(request, AUTHORIZATION_HEADER);
    }

    @Test
    void websocketChatPassesConversationRequestToExistingChatService() {
        UUID conversationId = UUID.randomUUID();
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(conversationId)
                .message("How many of them are active?")
                .build();
        Authentication authentication = authentication("teacher-user", "ROLE_TEACHER");
        when(aiChatService.chat(any(AiChatRequest.class), eq(AUTHORIZATION_HEADER))).thenReturn(AiChatResponse.builder()
                .message("I need a little more context before I can answer that follow-up.")
                .toolsUsed(List.of())
                .build());

        controller.chat(request, AUTHORIZATION_HEADER, authentication, headerAccessor());

        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService).chat(requestCaptor.capture(), eq(AUTHORIZATION_HEADER));
        assertThat(requestCaptor.getValue().getConversationId()).isEqualTo(conversationId);
        assertThat(requestCaptor.getValue().getMessage()).isEqualTo("How many of them are active?");
    }

    @Test
    void websocketChatRejectsStudentRoleBeforeCallingChatService() {
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(UUID.randomUUID())
                .message("What is inheritance?")
                .build();
        Authentication authentication = authentication("student-user", "ROLE_STUDENT");

        assertThatThrownBy(() -> controller.chat(request, AUTHORIZATION_HEADER, authentication, headerAccessor()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");

        verify(aiChatService, never()).chat(any(), any());
    }

    @Test
    void websocketChatSendsErrorResponseWhenProcessingFails() {
        UUID conversationId = UUID.randomUUID();
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(conversationId)
                .message("What is inheritance?")
                .build();
        Authentication authentication = authentication("admin-user", "ROLE_ADMIN");
        when(aiChatService.chat(request, AUTHORIZATION_HEADER)).thenThrow(new AccessDeniedException("Conversation not found"));

        assertThatThrownBy(() -> controller.chat(request, AUTHORIZATION_HEADER, authentication, headerAccessor()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Conversation not found");

        ArgumentCaptor<AiChatWebSocketResponse> responseCaptor = ArgumentCaptor.forClass(AiChatWebSocketResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq("admin-user"),
                eq("/queue/ai/chat"),
                responseCaptor.capture()
        );
        assertThat(responseCaptor.getAllValues())
                .extracting(AiChatWebSocketResponse::getStatus)
                .containsExactly("PROCESSING", "ERROR");
        assertThat(responseCaptor.getAllValues().get(1).getConversationId()).isEqualTo(conversationId);
        assertThat(responseCaptor.getAllValues().get(1).getMessage()).isEqualTo("Conversation not found");
    }

    @Test
    void websocketChatRequiresAuthenticatedPrincipal() {
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(UUID.randomUUID())
                .message("What is inheritance?")
                .build();

        assertThatThrownBy(() -> controller.chat(request, AUTHORIZATION_HEADER, null, headerAccessor()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");

        verify(aiChatService, never()).chat(any(), any());
    }

    private Authentication authentication(String name, String role) {
        return new UsernamePasswordAuthenticationToken(
                name,
                "n/a",
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private SimpMessageHeaderAccessor headerAccessor() {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionAttributes(new HashMap<>());
        return accessor;
    }
}
