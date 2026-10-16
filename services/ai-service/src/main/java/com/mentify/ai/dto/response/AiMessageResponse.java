package com.mentify.ai.dto.response;

import com.mentify.ai.enums.AiMessageRole;
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
public class AiMessageResponse {

    private UUID id;
    private AiMessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
