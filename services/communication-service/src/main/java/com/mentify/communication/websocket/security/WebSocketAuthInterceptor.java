package com.mentify.communication.websocket.security;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.communication.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_AUTHENTICATION_KEY = "mentify.websocket.authentication";
    private static final String USER_ID_HEADER = "x-user-id";

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
    private final GroupMemberService groupMemberService;
    private final WebSocketDestinationParser destinationParser;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        try {
            if (command == StompCommand.CONNECT) {
                Authentication authentication = authenticate(accessor);
                accessor.setUser(authentication);
                accessor.setLeaveMutable(true);
                return message;
            }

            if (command == StompCommand.SEND && isGroupSendDestination(accessor.getDestination())) {
                authorizeGroupDestination(accessor, destinationParser.parseSendGroupId(accessor.getDestination()));
                accessor.setLeaveMutable(true);
                return message;
            }

            if (command == StompCommand.SUBSCRIBE && isGroupSubscribeDestination(accessor.getDestination())) {
                authorizeGroupDestination(accessor, destinationParser.parseSubscribeGroupId(accessor.getDestination()));
                accessor.setLeaveMutable(true);
                return message;
            }

            if (command == StompCommand.DISCONNECT) {
                SecurityContextHolder.clearContext();
            }

            return message;
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
        String token = resolveBearerToken(accessor);
        Jwt jwt = jwtDecoder.decode(token);
        AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

        if (authentication == null) {
            throw new AccessDeniedException("Access denied");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        storeAuthentication(accessor, authentication);
        return authentication;
    }

    private void authorizeGroupDestination(StompHeaderAccessor accessor, UUID groupId) {
        Authentication authentication = requireAuthentication(accessor);
        UUID userId = resolveUserId(authentication);

        if (!groupMemberService.isActiveMember(groupId, userId)) {
            throw new AccessDeniedException("Access denied");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        accessor.setUser(authentication);
        accessor.setHeader(USER_ID_HEADER, userId.toString());
        storeAuthentication(accessor, authentication);
    }

    private Authentication requireAuthentication(StompHeaderAccessor accessor) {
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication == null && accessor.getSessionAttributes() != null) {
            Object sessionAuthentication = accessor.getSessionAttributes().get(SESSION_AUTHENTICATION_KEY);

            if (sessionAuthentication instanceof Authentication auth) {
                authentication = auth;
            }
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access denied");
        }

        return authentication;
    }

    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }

        throw new AccessDeniedException("Access denied");
    }

    private String resolveBearerToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(header)) {
            header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER.toLowerCase());
        }

        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Access denied");
        }

        return header.substring(BEARER_PREFIX.length()).trim();
    }

    private void storeAuthentication(StompHeaderAccessor accessor, Authentication authentication) {
        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put(SESSION_AUTHENTICATION_KEY, authentication);
        }
    }

    private boolean isGroupSendDestination(String destination) {
        return destination != null && destination.startsWith("/app/groups/");
    }

    private boolean isGroupSubscribeDestination(String destination) {
        return destination != null && destination.startsWith("/topic/groups/");
    }
}