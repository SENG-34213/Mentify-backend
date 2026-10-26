package com.mentify.ai.controller;

import com.mentify.ai.dto.tool.AiUsageOverview;
import com.mentify.ai.tool.AiUsageOverviewTool;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/usage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AiUsageController {

    private final AiUsageOverviewTool usageOverviewTool;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AiUsageOverview>> overview(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(ApiResponse.<AiUsageOverview>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("AI usage overview retrieved successfully")
                .data(usageOverviewTool.execute(authorizationHeader))
                .build());
    }
}
