package com.mentify.ai.dto.internal;

import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecutionRequest {
    private AiFeatureType featureType;
    private UUID userId;
    private AiExecutionContext context;
    private String systemPrompt;
    private String userInput;
    private String documentMimeType;
    private String documentDataBase64;
    private String documentFilename;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private AiResponseFormat responseFormat;
    private String traceId;
}
