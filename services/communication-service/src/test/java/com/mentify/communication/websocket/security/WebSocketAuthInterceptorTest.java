package com.mentify.communication.websocket.security;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.communication.service.GroupMemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Mock
    private GroupMemberService groupMemberService;

    @Spy
    private WebSocketDestinationParser destinationParser = new WebSocketDestinationParser();

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connectWithValidTokenSetsAuthentication() {
        Jwt jwt = jwt(UUID.randomUUID(), "student@mentify.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, jwt.getTokenValue(), List.of());

        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn((org.springframework.security.authentication.AbstractAuthenticationToken) authentication);

        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, "Bearer token", null);
        Message<?> result = interceptor.preSend(message, null);

        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtDecoder).decode("token");
    }

    @Test
    void sendToGroupRequiresMembership() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId, "student@mentify.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, jwt.getTokenValue(), List.of());
        when(groupMemberService.isActiveMember(groupId, userId)).thenReturn(true);

        Message<byte[]> message = stompMessage(
                StompCommand.SEND,
                "/app/groups/" + groupId + "/messages",
                null,
                authentication
        );

        Message<?> result = interceptor.preSend(message, null);

        assertSame(authentication, StompHeaderAccessor.wrap(result).getUser());
        verify(groupMemberService).isActiveMember(groupId, userId);
    }

    @Test
    void nonMemberCannotSendToGroup() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(userId, "student@mentify.com");
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, jwt.getTokenValue(), List.of());
        when(groupMemberService.isActiveMember(groupId, userId)).thenReturn(false);

        Message<byte[]> message = stompMessage(
                StompCommand.SEND,
                "/app/groups/" + groupId + "/messages",
                null,
                authentication
        );

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void taskFourTestDestinationBypassesGroupAuthorization() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/test", null, null);

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void invalidConnectTokenIsRejected() {
        when(jwtDecoder.decode("token")).thenThrow(new org.springframework.security.oauth2.jwt.JwtException("invalid"));

        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, "Bearer token", null);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, String authorizationHeader, Authentication user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(UUID.randomUUID().toString());
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        if (user != null) {
            accessor.setUser(user);
        }

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt(UUID userId, String username) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", List.of("STUDENT")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}