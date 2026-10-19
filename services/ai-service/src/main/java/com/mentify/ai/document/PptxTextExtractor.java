package com.mentify.ai.document;

import com.mentify.ai.exception.AiInvalidDocumentException;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class PptxTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        return "application/vnd.openxmlformats-officedocument.presentationml.presentation".equalsIgnoreCase(contentType)
                || hasExtension(filename, ".pptx");
    }

    @Override
    public String extract(byte[] content) {
        try (XMLSlideShow presentation = new XMLSlideShow(new ByteArrayInputStream(content));
             XSLFExtractor extractor = new XSLFExtractor(presentation)) {
            return extractor.getText();
        } catch (IOException ex) {
            throw new AiInvalidDocumentException("Unable to read PPTX document", ex);
        }
    }

    private boolean hasExtension(String filename, String extension) {
        return filename != null && filename.toLowerCase().endsWith(extension);
    }
}
