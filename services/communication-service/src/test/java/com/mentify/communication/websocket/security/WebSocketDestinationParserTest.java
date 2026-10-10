package com.mentify.communication.websocket.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketDestinationParserTest {

    private final WebSocketDestinationParser parser = new WebSocketDestinationParser();

    @Test
    void parsesSendDestinationGroupId() {
        UUID groupId = UUID.randomUUID();

        assertEquals(groupId, parser.parseSendGroupId("/app/groups/" + groupId + "/messages"));
    }

    @Test
    void parsesSubscribeDestinationGroupId() {
        UUID groupId = UUID.randomUUID();

        assertEquals(groupId, parser.parseSubscribeGroupId("/topic/groups/" + groupId));
    }

    @Test
    void rejectsInvalidDestination() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseSendGroupId("/app/test"));
    }
}