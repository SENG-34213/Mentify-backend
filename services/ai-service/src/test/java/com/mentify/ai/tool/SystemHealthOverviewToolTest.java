package com.mentify.ai.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemHealthOverviewToolTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @Test
    void returnsEmptyOverviewWhenNoServicesAreDiscoveredAndCachesItBriefly() {
        when(discoveryClient.getServices()).thenReturn(List.of());
        SystemHealthOverviewTool tool = new SystemHealthOverviewTool(discoveryClient);

        assertEquals(0, tool.execute("Bearer admin").totalServices());
        assertEquals(0, tool.execute("Bearer admin").totalServices());

        verify(discoveryClient, times(1)).getServices();
    }
}
