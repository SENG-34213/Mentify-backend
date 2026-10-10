package com.mentify.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotNull(message = "Conversation id is required")
    private UUID conversationId;

    @Size(max = 100, message = "Client message id must not exceed 100 characters")
    private String clientMessageId;

    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String message;
}
