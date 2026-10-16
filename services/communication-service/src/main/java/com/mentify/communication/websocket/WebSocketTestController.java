package com.mentify.communication.websocket;

import com.mentify.communication.dto.request.WebSocketTestMessageRequest;
import com.mentify.communication.dto.response.WebSocketTestMessageResponse;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketTestController {

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public WebSocketTestMessageResponse send(@Valid @Payload WebSocketTestMessageRequest request) {
        return WebSocketTestMessageResponse.builder()
                .content(request.getContent())
                .receivedAt(LocalDateTime.now())
                .build();
    }
}