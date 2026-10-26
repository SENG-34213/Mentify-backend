package com.mentify.ai.controller;

import com.mentify.ai.tool.AiToolDescriptor;
import com.mentify.ai.tool.AiToolRegistry;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/tools")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AiToolController {

    private final AiToolRegistry toolRegistry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiToolDescriptor>>> listTools() {
        return ResponseEntity.ok(ApiResponse.<List<AiToolDescriptor>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Available admin AI tools retrieved successfully")
                .data(toolRegistry.list())
                .build());
    }
}
