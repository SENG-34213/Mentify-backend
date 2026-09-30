package com.mentify.ai.websocket.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiWebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String SESSION_AUTHENTICATION_KEY = "mentify.ai.websocket.authentication";
    public static final String SESSION_AUTHORIZATION_HEADER_KEY = "mentify.ai.websocket.authorization";

    private static final String AUTHORIZATION_HEADER = "Authorization";

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

        Authentication authentication = resolveAuthentication(accessor);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        accessor.setUser(authentication);
        storeSessionValues(accessor, authentication, resolveAuthorizationHeader(accessor));
        accessor.setLeaveMutable(true);
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        SecurityContextHolder.clearContext();
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication && authentication.isAuthenticated()) {
            return authentication;
        }

        if (accessor.getSessionAttributes() != null) {
            Object sessionAuthentication = accessor.getSessionAttributes().get(SESSION_AUTHENTICATION_KEY);
            if (sessionAuthentication instanceof Authentication authentication && authentication.isAuthenticated()) {
                return authentication;
            }
        }

        throw new AccessDeniedException("Authentication is required");
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
}
