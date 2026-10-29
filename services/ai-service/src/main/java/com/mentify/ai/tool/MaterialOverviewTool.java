package com.mentify.ai.tool;

import com.mentify.ai.client.MaterialAdminClient;
import com.mentify.ai.client.dto.MaterialDetailsResponse;
import com.mentify.ai.dto.tool.MaterialOverview;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaterialOverviewTool implements AiTool<MaterialOverview> {

    public static final String TOOL_NAME = "getMaterialOverview";
    private static final int PAGE_SIZE = 50;

    private final MaterialAdminClient materialAdminClient;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public MaterialOverview execute(String authorizationHeader) {
        log.info("Executing AI tool {} pageSize={}", TOOL_NAME, PAGE_SIZE);
        return cache.get(TOOL_NAME, authorizationHeader, Duration.ofSeconds(30),
                () -> load(authorizationHeader));
    }

    private MaterialOverview load(String authorizationHeader) {
        ApiResponse<Page<MaterialDetailsResponse>> apiResponse =
                materialAdminClient.listMaterials(0, PAGE_SIZE, authorizationHeader);
        Page<MaterialDetailsResponse> page = apiResponse == null ? null : apiResponse.getData();
        if (page == null || page.getContent() == null) {
            return new MaterialOverview(0, 0, 0, 0, List.of());
        }

        long available = page.getContent().stream()
                .filter(material -> "AVAILABLE".equalsIgnoreCase(material.status()))
                .count();
        long bytes = page.getContent().stream()
                .map(MaterialDetailsResponse::fileSize)
                .filter(size -> size != null && size >= 0)
                .mapToLong(Long::longValue)
                .sum();
        List<String> titles = page.getContent().stream()
                .map(MaterialDetailsResponse::title)
                .filter(title -> title != null && !title.isBlank())
                .limit(10)
                .toList();

        return new MaterialOverview(page.getTotalElements(), page.getNumberOfElements(), available, bytes, titles);
    }
}
