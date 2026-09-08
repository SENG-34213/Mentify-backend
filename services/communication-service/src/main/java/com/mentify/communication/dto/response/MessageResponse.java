package com.mentify.communication.dto.response;

import com.mentify.communication.enums.MessageType;
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
public class MessageResponse {

    private UUID id;
    private UUID groupId;
    private UUID senderId;
    private String content;
    private MessageType type;
    private LocalDateTime sentAt;
}
