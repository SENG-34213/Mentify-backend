package com.mentify.ai.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentContentService {

    String extractReadableText(MultipartFile file, int requestedQuestionCount);
}
