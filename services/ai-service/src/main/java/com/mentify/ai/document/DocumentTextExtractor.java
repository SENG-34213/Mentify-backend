package com.mentify.ai.document;

public interface DocumentTextExtractor {

    boolean supports(String contentType, String filename);

    String extract(byte[] content);
}
