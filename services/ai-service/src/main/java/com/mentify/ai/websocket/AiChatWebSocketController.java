package com.mentify.ai.websocket;

import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.dto.response.AiChatWebSocketResponse;
import com.mentify.ai.exception.AiConversationNotFoundException;
import com.mentify.ai.exception.AiProviderException;
import com.mentify.ai.exception.AiProviderTimeoutException;
import com.mentify.ai.exception.AiProviderUnavailableException;
import com.mentify.ai.service.AiChatService;
import com.mentify.ai.websocket.security.AiWebSocketAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AiChatWebSocketController {

    private static final String RESPONSE_DESTINATION = "/queue/ai/chat";
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    private final AiChatService aiChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/ai/chat")
    public void chat(
            @Valid @Payload AiChatRequest request,
            @Header(name = "Authorization", required = false) String authorizationHeader,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Authentication authentication = requireAuthentication(principal, headerAccessor);
        assertAllowedRole(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String userDestination = authentication.getName();
        UUID conversationId = request.getConversationId();
        String clientMessageId = request.getClientMessageId();
        log.info("Received websocket AI chat request conversationId={}", conversationId);
        sendStatus(userDestination, conversationId, null, clientMessageId, "PROCESSING", "AI chat request is being processed", null);

        try {
            AiChatResponse response = aiChatService.chat(request, resolveAuthorizationHeader(authorizationHeader, headerAccessor));
            sendStatus(
                    userDestination,
                    conversationId,
                    response.getMessageId(),
                    response.getClientMessageId(),
                    "COMPLETED",
                    "AI chat response generated successfully",
                    response
            );
        } catch (RuntimeException ex) {
            sendError(userDestination, conversationId, clientMessageId, ex);
            throw ex;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception ex, Principal principal) {
        if (principal == null) {
            log.warn("Websocket AI chat error without principal: {}", ex.getMessage());
            return;
        }

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                RESPONSE_DESTINATION,
                AiChatWebSocketResponse.builder()
                        .status("ERROR")
                        .message(safeErrorMessage(ex))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    private Authentication requireAuthentication(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal instanceof Authentication authentication && authentication.isAuthenticated()) {
            return authentication;
        }

        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object authentication = headerAccessor.getSessionAttributes()
                    .get(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY);
            if (authentication instanceof Authentication auth && auth.isAuthenticated()) {
                return auth;
            }
        }

        Authentication securityContextAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (securityContextAuthentication != null && securityContextAuthentication.isAuthenticated()) {
            return securityContextAuthentication;
        }

        throw new AccessDeniedException("Authentication is required");
    }

    private void assertAllowedRole(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private String resolveAuthorizationHeader(String authorizationHeader, SimpMessageHeaderAccessor headerAccessor) {
        if (StringUtils.hasText(authorizationHeader)) {
            return authorizationHeader;
        }

        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object storedAuthorization = headerAccessor.getSessionAttributes()
                    .get(AiWebSocketAuthInterceptor.SESSION_AUTHORIZATION_HEADER_KEY);
            if (storedAuthorization instanceof String storedHeader) {
                return storedHeader;
            }
        }

        return null;
    }

    private void sendStatus(
            String userDestination,
            UUID conversationId,
            UUID messageId,
            String clientMessageId,
            String status,
            String message,
            AiChatResponse data
    ) {
        messagingTemplate.convertAndSendToUser(
                userDestination,
                RESPONSE_DESTINATION,
                AiChatWebSocketResponse.builder()
                        .conversationId(conversationId)
                        .messageId(messageId)
                        .clientMessageId(clientMessageId)
                        .status(status)
                        .message(message)
                        .data(data)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    private void sendError(String userDestination, UUID conversationId, String clientMessageId, RuntimeException ex) {
        messagingTemplate.convertAndSendToUser(
                userDestination,
                RESPONSE_DESTINATION,
                AiChatWebSocketResponse.builder()
                        .conversationId(conversationId)
                        .clientMessageId(clientMessageId)
                        .status("ERROR")
                        .message(safeErrorMessage(ex))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    private String safeErrorMessage(Exception ex) {
        if (ex instanceof AccessDeniedException) {
            return "Access denied";
        }
        if (ex instanceof AiConversationNotFoundException) {
            return "AI conversation not found";
        }
        if (ex instanceof AiProviderTimeoutException) {
            return "AI provider timed out. Please try again.";
        }
        if (ex instanceof AiProviderUnavailableException) {
            return "AI provider is currently unavailable. Please try again later.";
        }
        if (ex instanceof AiProviderException) {
            return "AI provider request failed. Please try again.";
        }
        return "AI chat request failed";
    }
}
