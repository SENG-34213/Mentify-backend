package com.mentify.communication.service;

import com.mentify.communication.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Publishes communication messages using the same payload for REST and STOMP sends. */
@Service
@RequiredArgsConstructor
public class MessageBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(UUID groupId, MessageResponse message) {
        messagingTemplate.convertAndSend("/topic/groups/" + groupId, message);
    }
}
