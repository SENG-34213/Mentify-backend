package com.mentify.ai.tool;

import com.mentify.ai.client.ExamAnalyticsClient;
import com.mentify.ai.dto.tool.ExamSummaryToolResult;
import com.mentify.ai.dto.tool.StudentExamResultsToolResult;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamAnalyticsToolsTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private ExamAnalyticsClient examAnalyticsClient;

    @Test
    void myExamResultsToolForwardsAuthorizationHeader() {
        StudentExamResultsToolResult results =
                StudentExamResultsToolResult.builder().results(List.of()).build();
        when(examAnalyticsClient.getMyExamResults(AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", results));

        MyExamResultsTool tool = new MyExamResultsTool(examAnalyticsClient);

        assertThat(tool.getName()).isEqualTo(MyExamResultsTool.TOOL_NAME);
        assertThat(tool.execute(AUTHORIZATION_HEADER)).isSameAs(results);
        verify(examAnalyticsClient).getMyExamResults(AUTHORIZATION_HEADER);
    }

    @Test
    void examSummaryToolForwardsExamIdAndAuthorizationHeader() {
        UUID examId = UUID.randomUUID();
        ExamSummaryToolResult summary = ExamSummaryToolResult.builder().examId(examId).build();
        when(examAnalyticsClient.getExamSummary(examId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", summary));

        ExamSummaryTool tool = new ExamSummaryTool(examAnalyticsClient);

        assertThat(tool.getName()).isEqualTo(ExamSummaryTool.TOOL_NAME);
        assertThat(tool.execute(examId, AUTHORIZATION_HEADER)).isSameAs(summary);
        verify(examAnalyticsClient).getExamSummary(examId, AUTHORIZATION_HEADER);
    }
}
