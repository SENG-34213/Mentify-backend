package com.mentify.ai.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiToolRegistryTest {

    @Test
    void resolvesTheMostSpecificRegisteredAdminIntent() {
        AiToolRegistry registry = new AiToolRegistry(List.of(
                tool("getMaterialOverview"),
                tool("getNotificationOverview"),
                tool("getAiUsageOverview")
        ));

        assertEquals("getAiUsageOverview",
                registry.resolveAdminIntent("Show failed AI requests and AI latency").orElseThrow().name());
        assertEquals("getNotificationOverview",
                registry.resolveAdminIntent("Show failed email delivery notifications").orElseThrow().name());
    }

    @Test
    void ignoresUnregisteredOrUnmatchedIntent() {
        AiToolRegistry registry = new AiToolRegistry(List.of(tool("getMaterialOverview")));

        assertTrue(registry.resolveAdminIntent("Show payment failures").isEmpty());
    }

    @Test
    void doesNotExposeStudentSelfProgressAsAnAdminTool() {
        AiToolRegistry registry = new AiToolRegistry(List.of(tool("getStudentProgress")));

        assertFalse(registry.list().stream().anyMatch(tool -> tool.name().equals("getStudentProgress")));
        assertTrue(registry.resolveAdminIntent("Show my progress").isEmpty());
    }

    private AiTool<?> tool(String name) {
        return new AiTool<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Object execute(String authorizationHeader) {
                return null;
            }
        };
    }
}
