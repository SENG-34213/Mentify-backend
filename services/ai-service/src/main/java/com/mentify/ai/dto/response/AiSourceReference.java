package com.mentify.ai.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AiSourceReference {
    UUID materialId;
    String title;
    String originalFilename;
}
