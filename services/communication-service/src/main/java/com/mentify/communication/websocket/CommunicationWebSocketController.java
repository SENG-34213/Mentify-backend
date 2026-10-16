package com.mentify.communication.websocket;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CommunicationWebSocketController {

    private static final String SESSION_AUTHENTICATION_KEY = "mentify.websocket.authentication";

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/groups/{groupId}/messages")
    public void sendMessage(
            @DestinationVariable UUID groupId,
            @Valid @Payload SendMessageRequest request,
            @Header(name = "x-user-id", required = false) String senderIdHeader,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID senderId = resolveSenderId(senderIdHeader, principal, headerAccessor);
        MessageResponse response = messageService.sendMessage(groupId, request, senderId);
        messagingTemplate.convertAndSend("/topic/groups/" + groupId, response);
    }

    private UUID resolveSenderId(String senderIdHeader, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (senderIdHeader != null && !senderIdHeader.isBlank()) {
            return UUID.fromString(senderIdHeader);
        }

        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object authentication = headerAccessor.getSessionAttributes().get(SESSION_AUTHENTICATION_KEY);
            if (authentication instanceof Authentication auth && auth.getPrincipal() instanceof Jwt jwt) {
                return UUID.fromString(jwt.getSubject());
            }
        }

        return extractUserId(principal);
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }

        throw new AccessDeniedException("Access denied");
    }
}