package com.mentify.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatWebSocketResponse {

    private UUID conversationId;
    private String status;
    private String message;
    private AiChatResponse data;
    private LocalDateTime timestamp;
}
