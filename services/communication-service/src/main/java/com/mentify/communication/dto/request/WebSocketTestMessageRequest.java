package com.mentify.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketTestMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 200, message = "Message content must not exceed 200 characters")
    private String content;
}