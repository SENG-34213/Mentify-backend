package com.mentify.ai.service.impl;

import com.mentify.ai.config.QuizGenerationProperties;
import com.mentify.ai.document.DocumentTextExtractor;
import com.mentify.ai.exception.AiInvalidDocumentException;
import com.mentify.ai.service.DocumentContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentContentServiceImpl implements DocumentContentService {

    private final List<DocumentTextExtractor> extractors;
    private final QuizGenerationProperties properties;

    @Override
    public String extractReadableText(MultipartFile file, int requestedQuestionCount) {
        validateFile(file);
        DocumentTextExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(file.getContentType(), file.getOriginalFilename()))
                .findFirst()
                .orElseThrow(() -> new AiInvalidDocumentException("Unsupported document type. Upload PDF, DOCX, or TXT"));

        byte[] content = readFile(file);
        String normalized = normalize(extractor.extract(content));
        validateReadableContent(normalized, requestedQuestionCount);
        return boundContent(normalized);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AiInvalidDocumentException("Document file is required");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new AiInvalidDocumentException("Document exceeds the maximum allowed file size");
        }
    }

    private byte[] readFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new AiInvalidDocumentException("Unable to read uploaded document", ex);
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validateReadableContent(String text, int requestedQuestionCount) {
        if (text.isBlank()) {
            throw new AiInvalidDocumentException("No readable text could be extracted from the document");
        }
        int requiredCharacters = Math.max(1, requestedQuestionCount) * properties.getMinDocumentCharactersPerQuestion();
        if (text.length() < requiredCharacters) {
            throw new AiInvalidDocumentException("Document does not contain enough readable educational text for the requested number of questions");
        }
    }

    private String boundContent(String text) {
        if (text.length() <= properties.getMaxDocumentCharacters()) {
            return text;
        }
        return text.substring(0, properties.getMaxDocumentCharacters());
    }
}
