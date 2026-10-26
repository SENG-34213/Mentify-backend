package com.mentify.ai.tool;

import java.util.Set;

/**
 * Stable metadata for an AI tool. The registry is deliberately metadata-only;
 * tool execution still happens through typed Java services and authenticated
 * downstream clients.
 */
public record AiToolDescriptor(
        String name,
        String description,
        String service,
        Set<String> allowedRoles,
        boolean readOnly,
        Set<String> triggerPhrases
) {

    public AiToolDescriptor(
            String name,
            String description,
            String service,
            Set<String> allowedRoles,
            boolean readOnly
    ) {
        this(name, description, service, allowedRoles, readOnly, Set.of());
    }
}
