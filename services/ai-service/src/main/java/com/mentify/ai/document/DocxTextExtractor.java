package com.mentify.ai.document;

import com.mentify.ai.exception.AiInvalidDocumentException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class DocxTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType)
                || hasExtension(filename, ".docx");
    }

    @Override
    public String extract(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException ex) {
            throw new AiInvalidDocumentException("Unable to read DOCX document", ex);
        }
    }

    private boolean hasExtension(String filename, String extension) {
        return filename != null && filename.toLowerCase().endsWith(extension);
    }
}
