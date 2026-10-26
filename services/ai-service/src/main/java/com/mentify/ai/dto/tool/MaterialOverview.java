package com.mentify.ai.dto.tool;

import java.util.List;

public record MaterialOverview(
        long totalMaterials,
        long returnedMaterials,
        long availableMaterials,
        long totalBytesOnPage,
        List<String> recentTitles
) {
}
