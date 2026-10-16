package com.mentify.ai.service.impl;

import com.mentify.ai.config.QuizGenerationProperties;
import com.mentify.ai.document.DocxTextExtractor;
import com.mentify.ai.document.PdfTextExtractor;
import com.mentify.ai.document.TxtTextExtractor;
import com.mentify.ai.exception.AiInvalidDocumentException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentContentServiceImplTest {

    private QuizGenerationProperties properties;
    private DocumentContentServiceImpl documentContentService;

    @BeforeEach
    void setUp() {
        properties = new QuizGenerationProperties();
        properties.setMinDocumentCharactersPerQuestion(10);
        documentContentService = new DocumentContentServiceImpl(
                List.of(new PdfTextExtractor(), new DocxTextExtractor(), new TxtTextExtractor()),
                properties
        );
    }

    @Test
    void extractsReadableTextFromPdf() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oop.pdf",
                "application/pdf",
                readablePdf("Encapsulation protects internal object state and exposes behavior through methods.")
        );

        String text = documentContentService.extractReadableText(file, 1);

        assertThat(text).contains("Encapsulation protects internal object state");
    }

    @Test
    void rejectsUnsupportedFormat() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "not supported".getBytes()
        );

        assertThatThrownBy(() -> documentContentService.extractReadableText(file, 1))
                .isInstanceOf(AiInvalidDocumentException.class)
                .hasMessageContaining("Unsupported document type");
    }

    @Test
    void rejectsEmptyExtractedText() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                "   ".getBytes()
        );

        assertThatThrownBy(() -> documentContentService.extractReadableText(file, 1))
                .isInstanceOf(AiInvalidDocumentException.class)
                .hasMessageContaining("No readable text");
    }

    private byte[] readablePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
