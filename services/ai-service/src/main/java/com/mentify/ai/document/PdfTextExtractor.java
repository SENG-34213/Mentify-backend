package com.mentify.ai.document;

import com.mentify.ai.exception.AiInvalidDocumentException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || hasExtension(filename, ".pdf");
    }

    @Override
    public String extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new AiInvalidDocumentException("Encrypted PDF documents are not supported");
            }
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new AiInvalidDocumentException("Unable to read PDF document", ex);
        }
    }

    private boolean hasExtension(String filename, String extension) {
        return filename != null && filename.toLowerCase().endsWith(extension);
    }
}
