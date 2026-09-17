package com.mentify.ai.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResponse {
 
    private String content;
    private String provider;
    private String model;
    private LocalDateTime generatedAt;
}
