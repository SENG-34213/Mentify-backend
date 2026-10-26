package com.mentify.ai.tool;

import com.mentify.ai.client.MaterialAdminClient;
import com.mentify.ai.client.dto.MaterialDetailsResponse;
import com.mentify.ai.dto.tool.MaterialOverview;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialOverviewToolTest {

    @Mock
    private MaterialAdminClient materialAdminClient;

    @Test
    void summarizesOnlyTheBoundedMaterialPage() {
        MaterialDetailsResponse available = new MaterialDetailsResponse(
                UUID.randomUUID(), "Guide", null, "guide.pdf", "application/pdf", 120L,
                "DOCUMENT", "LEARNING_MATERIAL", "AVAILABLE", UUID.randomUUID(), null,
                "checksum-1", LocalDateTime.now(), LocalDateTime.now());
        MaterialDetailsResponse unavailable = new MaterialDetailsResponse(
                UUID.randomUUID(), "Old guide", null, "old.pdf", "application/pdf", 80L,
                "DOCUMENT", "LEARNING_MATERIAL", "DELETED", UUID.randomUUID(), null,
                "checksum-2", LocalDateTime.now(), LocalDateTime.now());
        when(materialAdminClient.listMaterials(0, 50, "Bearer token"))
                .thenReturn(ApiResponse.success(200, "ok", new PageImpl<>(
                        List.of(available, unavailable), PageRequest.of(0, 50), 75)));

        MaterialOverview result = new MaterialOverviewTool(materialAdminClient).execute("Bearer token");

        assertThat(result.totalMaterials()).isEqualTo(75);
        assertThat(result.returnedMaterials()).isEqualTo(2);
        assertThat(result.availableMaterials()).isEqualTo(1);
        assertThat(result.totalBytesOnPage()).isEqualTo(200);
        assertThat(result.recentTitles()).containsExactly("Guide", "Old guide");
        verify(materialAdminClient).listMaterials(0, 50, "Bearer token");
    }
}
