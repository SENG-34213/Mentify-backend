package com.mentify.ai.websocket.security;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    private final MessageChannel channel = mock(MessageChannel.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connectWithValidJwtSetsAuthenticatedPrincipalAndStoresAuthorizationHeader() {
        Jwt jwt = jwt(UUID.randomUUID(), "admin@mentify.com", "ADMIN");
        AbstractAuthenticationToken authentication = authentication(jwt, "admin-user", "ROLE_ADMIN");
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);
        accessor.setNativeHeader("Authorization", "Bearer token");

        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(authentication);

        Message<?> message = interceptor.preSend(message(accessor), channel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(message);
        assertThat(resultAccessor.getUser()).isEqualTo(authentication);
        assertThat(accessor.getSessionAttributes())
                .containsEntry(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication)
                .containsEntry(AiWebSocketAuthInterceptor.SESSION_AUTHORIZATION_HEADER_KEY, "Bearer token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(jwtDecoder).decode("token");
    }

    @Test
    void invalidConnectJwtIsRejectedSafely() {
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);
        accessor.setNativeHeader("Authorization", "Bearer token");
        when(jwtDecoder.decode("token")).thenThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void expiredConnectJwtIsRejectedSafely() {
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);
        accessor.setNativeHeader("Authorization", "Bearer token");
        JwtValidationException expired = new JwtValidationException(
                "expired",
                List.of(new OAuth2Error("invalid_token", "Jwt expired", null))
        );
        when(jwtDecoder.decode("token")).thenThrow(expired);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void connectWithoutJwtIsRejectedSafely() {
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void unauthorizedRoleCannotConnectToAiChat() {
        Jwt jwt = jwt(UUID.randomUUID(), "student@mentify.com", "STUDENT");
        AbstractAuthenticationToken authentication = authentication(jwt, "student-user", "ROLE_STUDENT");
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null);
        accessor.setNativeHeader("Authorization", "Bearer token");

        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(authentication);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void sendRestoresAuthenticationFromSessionForAiChatDestination() {
        Authentication authentication = authentication("teacher-user", "ROLE_TEACHER");
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, "/app/ai/chat");
        accessor.getSessionAttributes().put(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication);

        Message<?> message = interceptor.preSend(message(accessor), channel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(message);
        assertThat(resultAccessor.getUser()).isEqualTo(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void sendWithoutAuthenticationIsRejected() {
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, "/app/ai/chat");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void authenticatedUserCanSubscribeOnlyToUserSpecificAiQueue() {
        Authentication authentication = authentication("admin-user", "ROLE_ADMIN");
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE, "/user/queue/ai/chat");
        accessor.getSessionAttributes().put(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication);

        Message<?> message = interceptor.preSend(message(accessor), channel);

        assertThat(StompHeaderAccessor.wrap(message).getUser()).isEqualTo(authentication);
    }

    @Test
    void rawQueueSubscriptionIsRejectedToPreventCrossUserMessageAccess() {
        Authentication authentication = authentication("admin-user", "ROLE_ADMIN");
        AiWebSocketAuthInterceptor interceptor = interceptor();
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE, "/queue/ai/chat");
        accessor.getSessionAttributes().put(AiWebSocketAuthInterceptor.SESSION_AUTHENTICATION_KEY, authentication);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    private AiWebSocketAuthInterceptor interceptor() {
        return new AiWebSocketAuthInterceptor(jwtDecoder, jwtAuthenticationConverter);
    }

    private StompHeaderAccessor accessor(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(UUID.randomUUID().toString());
        accessor.setSessionAttributes(new HashMap<>());
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private AbstractAuthenticationToken authentication(Jwt jwt, String name, String role) {
        return new UsernamePasswordAuthenticationToken(
                name,
                jwt.getTokenValue(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private Authentication authentication(String name, String role) {
        return new UsernamePasswordAuthenticationToken(
                name,
                "n/a",
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private Jwt jwt(UUID userId, String username, String role) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
