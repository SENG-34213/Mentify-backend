package com.mentify.ai.websocket.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AiWebSocketAuthInterceptorTest {

    private final AiWebSocketAuthInterceptor interceptor = new AiWebSocketAuthInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connectStoresAuthenticatedPrincipalAndAuthorizationHeader() {
        Authentication authentication = authentication("admin-user");
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(authentication);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setNativeHeader("Authorization", "Bearer token");

        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel);

        assertThat(accessor.getSessionAttributes())
                .containsEntry(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication)
                .containsEntry(AiWebSocketAuthInterceptor.SESSION_AUTHORIZATION_HEADER_KEY, "Bearer token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void sendRestoresAuthenticationFromSession() {
        Authentication authentication = authentication("teacher-user");
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication);

        Message<?> message = interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()),
                channel
        );

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(message);
        assertThat(resultAccessor.getUser()).isEqualTo(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void sendWithoutAuthenticationIsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionAttributes(new HashMap<>());

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
    }

    private Authentication authentication(String name) {
        return new UsernamePasswordAuthenticationToken(
                name,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
