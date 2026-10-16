package com.mentify.communication.websocket.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebSocketDestinationParser {

    private static final String SEND_PREFIX = "/app/groups/";
    private static final String SEND_SUFFIX = "/messages";
    private static final String SUBSCRIBE_PREFIX = "/topic/groups/";

    public UUID parseSendGroupId(String destination) {
        return parseGroupId(destination, SEND_PREFIX, SEND_SUFFIX);
    }

    public UUID parseSubscribeGroupId(String destination) {
        return parseGroupId(destination, SUBSCRIBE_PREFIX, "");
    }

    private UUID parseGroupId(String destination, String prefix, String suffix) {
        if (destination == null || !destination.startsWith(prefix) || !destination.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid WebSocket destination");
        }

        String groupIdValue = destination.substring(prefix.length(), destination.length() - suffix.length());

        if (groupIdValue.isBlank() || groupIdValue.contains("/")) {
            throw new IllegalArgumentException("Invalid WebSocket destination");
        }

        return UUID.fromString(groupIdValue);
    }
}