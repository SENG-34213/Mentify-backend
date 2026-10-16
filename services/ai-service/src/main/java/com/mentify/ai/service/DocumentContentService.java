package com.mentify.ai.service;

import com.mentify.ai.dto.internal.MaterialDocument;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentContentService {

    String extractReadableText(MultipartFile file, int requestedQuestionCount);

    String extractReadableText(MaterialDocument document, int requestedQuestionCount);

    String extractReadableText(MaterialDocument document);
}
