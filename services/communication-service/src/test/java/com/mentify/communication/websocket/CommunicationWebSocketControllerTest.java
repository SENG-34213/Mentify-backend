package com.mentify.communication.websocket;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.enums.MessageType;
import com.mentify.communication.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationWebSocketControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CommunicationWebSocketController controller;

    @Test
    void sendMessagePersistsAndBroadcastsToGroupTopic() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageResponse response = messageResponse(groupId, "Hello group");
        Authentication principal = new UsernamePasswordAuthenticationToken(
            jwt(senderId),
            "token",
            List.of()
        );

        when(messageService.sendMessage(eq(groupId), org.mockito.ArgumentMatchers.any(SendMessageRequest.class), eq(senderId)))
                .thenReturn(response);

        controller.sendMessage(
            groupId,
            SendMessageRequest.builder().content("Hello group").build(),
            senderId.toString(),
            principal,
            SimpMessageHeaderAccessor.create()
        );

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageResponse> payloadCaptor = ArgumentCaptor.forClass(MessageResponse.class);

        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), payloadCaptor.capture());
        assertEquals("/topic/groups/" + groupId, topicCaptor.getValue());
        assertEquals(response, payloadCaptor.getValue());
        verify(messageService).sendMessage(eq(groupId), org.mockito.ArgumentMatchers.any(SendMessageRequest.class), eq(senderId));
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("preferred_username", "teacher@mentify.com")
                .claim("realm_access", Map.of("roles", List.of("TEACHER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private MessageResponse messageResponse(UUID groupId, String content) {
        return MessageResponse.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .senderId(UUID.randomUUID())
                .content(content)
                .type(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();
    }
}