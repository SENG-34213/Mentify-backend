package com.mentify.ai.websocket.security;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiWebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String SESSION_AUTHENTICATION_KEY = "mentify.ai.websocket.authentication";
    public static final String SESSION_AUTHORIZATION_HEADER_KEY = "mentify.ai.websocket.authorization";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHAT_SEND_DESTINATION = "/app/ai/chat";
    private static final String CHAT_SUBSCRIBE_DESTINATION = "/user/queue/ai/chat";
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (command == StompCommand.DISCONNECT) {
            SecurityContextHolder.clearContext();
            return message;
        }

        try {
            if (command == StompCommand.CONNECT) {
                Authentication authentication = authenticate(accessor);
                accessor.setUser(authentication);
                accessor.setLeaveMutable(true);
                return rebuildMessage(message, accessor);
            }

            Authentication authentication = requireAuthentication(accessor);
            assertAllowedRole(authentication);
            assertAllowedDestination(command, accessor.getDestination());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);
            storeSessionValues(accessor, authentication, resolveAuthorizationHeader(accessor));
            accessor.setLeaveMutable(true);
            return rebuildMessage(message, accessor);
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Access denied", ex);
        }
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = resolveAuthorizationHeader(accessor);
        String token = resolveBearerToken(authorizationHeader);
        Jwt jwt = jwtDecoder.decode(token);
        AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access denied");
        }

        assertAllowedRole(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        storeSessionValues(accessor, authentication, authorizationHeader);
        return authentication;
    }

    private Authentication requireAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication && authentication.isAuthenticated()) {
            return authentication;
        }

        if (accessor.getSessionAttributes() != null) {
            Object sessionAuthentication = accessor.getSessionAttributes().get(SESSION_AUTHENTICATION_KEY);
            if (sessionAuthentication instanceof Authentication authentication && authentication.isAuthenticated()) {
                return authentication;
            }
        }

        throw new AccessDeniedException("Access denied");
    }

    private void assertAllowedRole(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void assertAllowedDestination(StompCommand command, String destination) {
        if (command == StompCommand.SEND && !CHAT_SEND_DESTINATION.equals(destination)) {
            throw new AccessDeniedException("Access denied");
        }

        if (command == StompCommand.SUBSCRIBE && !CHAT_SUBSCRIBE_DESTINATION.equals(destination)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)) {
            authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER.toLowerCase());
        }

        if (!StringUtils.hasText(authorizationHeader) && accessor.getSessionAttributes() != null) {
            Object storedAuthorization = accessor.getSessionAttributes().get(SESSION_AUTHORIZATION_HEADER_KEY);
            if (storedAuthorization instanceof String storedHeader) {
                authorizationHeader = storedHeader;
            }
        }

        return authorizationHeader;
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Access denied");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new AccessDeniedException("Access denied");
        }

        return token;
    }

    private void storeSessionValues(
            StompHeaderAccessor accessor,
            Authentication authentication,
            String authorizationHeader
    ) {
        if (accessor.getSessionAttributes() == null) {
            return;
        }

        accessor.getSessionAttributes().put(SESSION_AUTHENTICATION_KEY, authentication);
        if (StringUtils.hasText(authorizationHeader)) {
            accessor.getSessionAttributes().put(SESSION_AUTHORIZATION_HEADER_KEY, authorizationHeader);
        }
    }

    private Message<?> rebuildMessage(Message<?> message, StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
