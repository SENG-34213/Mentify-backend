package com.mentify.ai.dto.request;
 
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
public class AiGenerateRequest {
 
    @NotBlank(message = "Prompt cannot be blank")
    @Size(max = 5000, message = "Prompt is too long (max 5000 characters)")
    private String prompt;
}
