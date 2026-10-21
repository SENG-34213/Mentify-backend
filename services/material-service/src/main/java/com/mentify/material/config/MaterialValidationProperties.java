package com.mentify.material.config;

import com.mentify.material.enums.MaterialType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "material.validation")
public class MaterialValidationProperties {

    private DataSize maxUploadSize = DataSize.ofMegabytes(100);
    private List<FileTypeRule> fileTypes = defaultFileTypes();

    @Getter
    @Setter
    public static class FileTypeRule {
        private MaterialType materialType;
        private List<String> extensions = new ArrayList<>();
        private List<String> contentTypes = new ArrayList<>();

        public FileTypeRule() {
        }

        public FileTypeRule(MaterialType materialType, List<String> extensions, List<String> contentTypes) {
            this.materialType = materialType;
            this.extensions = extensions;
            this.contentTypes = contentTypes;
        }
    }

    private static List<FileTypeRule> defaultFileTypes() {
        return List.of(
                new FileTypeRule(MaterialType.PDF, List.of("pdf"), List.of("application/pdf")),
                new FileTypeRule(MaterialType.DOCUMENT, List.of("doc"), List.of("application/msword")),
                new FileTypeRule(MaterialType.DOCUMENT, List.of("docx"), List.of(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )),
                new FileTypeRule(MaterialType.DOCUMENT, List.of("txt"), List.of("text/plain")),
                new FileTypeRule(MaterialType.TEXT, List.of("md", "markdown"), List.of("text/markdown", "text/plain")),
                new FileTypeRule(MaterialType.TEXT, List.of("csv"), List.of("text/csv", "application/csv", "text/plain")),
                new FileTypeRule(MaterialType.TEXT, List.of("rtf"), List.of("application/rtf", "text/rtf")),
                new FileTypeRule(MaterialType.DOCUMENT, List.of("odt"), List.of("application/vnd.oasis.opendocument.text")),
                new FileTypeRule(MaterialType.PRESENTATION, List.of("ppt"), List.of("application/vnd.ms-powerpoint")),
                new FileTypeRule(MaterialType.PRESENTATION, List.of("pptx"), List.of(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                )),
                new FileTypeRule(MaterialType.PRESENTATION, List.of("odp"), List.of("application/vnd.oasis.opendocument.presentation")),
                new FileTypeRule(MaterialType.SPREADSHEET, List.of("xls"), List.of("application/vnd.ms-excel")),
                new FileTypeRule(MaterialType.SPREADSHEET, List.of("xlsx"), List.of(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )),
                new FileTypeRule(MaterialType.SPREADSHEET, List.of("ods"), List.of("application/vnd.oasis.opendocument.spreadsheet")),
                new FileTypeRule(MaterialType.IMAGE, List.of("jpg", "jpeg"), List.of("image/jpeg")),
                new FileTypeRule(MaterialType.IMAGE, List.of("png"), List.of("image/png")),
                new FileTypeRule(MaterialType.IMAGE, List.of("gif"), List.of("image/gif")),
                new FileTypeRule(MaterialType.IMAGE, List.of("webp"), List.of("image/webp")),
                new FileTypeRule(MaterialType.VIDEO, List.of("mp4"), List.of("video/mp4")),
                new FileTypeRule(MaterialType.VIDEO, List.of("mov"), List.of("video/quicktime")),
                new FileTypeRule(MaterialType.VIDEO, List.of("webm"), List.of("video/webm")),
                new FileTypeRule(MaterialType.AUDIO, List.of("webm"), List.of("audio/webm")),
                new FileTypeRule(MaterialType.AUDIO, List.of("mp3"), List.of("audio/mpeg")),
                new FileTypeRule(MaterialType.AUDIO, List.of("wav"), List.of("audio/wav", "audio/x-wav")),
                new FileTypeRule(MaterialType.AUDIO, List.of("m4a"), List.of("audio/mp4")),
                new FileTypeRule(MaterialType.AUDIO, List.of("ogg"), List.of("audio/ogg"))
        );
    }
}
