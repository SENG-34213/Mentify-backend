package com.mentify.ai.controller;
 
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.service.AiService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGenerationController {
 
    private final AiService aiService;
 
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(@Valid @RequestBody AiGenerateRequest request) {
        log.info("Received AI generation request");
        AiGenerateResponse response = aiService.generate(request);
        return ResponseEntity.ok(ApiResponse.<AiGenerateResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("AI content generated successfully")
                .data(response)
                .build());
    }
}
