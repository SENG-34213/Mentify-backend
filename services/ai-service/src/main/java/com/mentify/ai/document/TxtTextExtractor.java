package com.mentify.ai.document;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        return "text/plain".equalsIgnoreCase(contentType)
                || hasExtension(filename, ".txt");
    }

    @Override
    public String extract(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private boolean hasExtension(String filename, String extension) {
        return filename != null && filename.toLowerCase().endsWith(extension);
    }
}
