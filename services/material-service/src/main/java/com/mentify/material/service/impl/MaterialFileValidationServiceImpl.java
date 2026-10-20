package com.mentify.material.service.impl;

import com.mentify.material.config.MaterialValidationProperties;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.service.MaterialFileValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialFileValidationServiceImpl implements MaterialFileValidationService {

    private static final int HEADER_BYTES = 512;
    private static final int MAX_FILENAME_LENGTH = 255;

    private final MaterialValidationProperties properties;

    @Override
    public ValidatedMaterialFile validate(MultipartFile file) {
        if (file == null) {
            throw new MaterialValidationException("File is required");
        }
        if (file.isEmpty() || file.getSize() <= 0) {
            throw new MaterialValidationException("File must not be empty");
        }
        if (file.getSize() > properties.getMaxUploadSize().toBytes()) {
            throw new MaterialValidationException("File exceeds the maximum allowed upload size");
        }

        String originalFilename = requireSafeOriginalFilename(file.getOriginalFilename());
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String extension = extractExtension(sanitizedFilename);
        String contentType = normalizeContentType(file.getContentType());
        MaterialValidationProperties.FileTypeRule rule = findRule(extension, contentType)
                .orElseThrow(() -> new MaterialValidationException("Unsupported file type"));

        byte[] header = readHeader(file);
        if (!signatureMatches(extension, contentType, header)) {
            throw new MaterialValidationException("File content does not match the declared file type");
        }

        return new ValidatedMaterialFile(
                originalFilename,
                sanitizedFilename,
                extension,
                contentType,
                file.getSize(),
                rule.getMaterialType()
        );
    }

    private Optional<MaterialValidationProperties.FileTypeRule> findRule(String extension, String contentType) {
        return properties.getFileTypes().stream()
                .filter(rule -> normalized(rule.getExtensions()).contains(extension))
                .filter(rule -> normalized(rule.getContentTypes()).contains(contentType))
                .findFirst();
    }

    private Set<String> normalized(Iterable<String> values) {
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String requireSafeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new MaterialValidationException("Original filename is required");
        }
        if (originalFilename.indexOf('\0') >= 0
                || originalFilename.contains("/")
                || originalFilename.contains("\\")
                || originalFilename.contains("..")) {
            throw new MaterialValidationException("Filename is not safe");
        }
        String trimmed = originalFilename.trim();
        if (trimmed.equals(".") || trimmed.equals("..")) {
            throw new MaterialValidationException("Filename is not safe");
        }
        return trimmed;
    }

    private String sanitizeFilename(String originalFilename) {
        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^\\.+", "");

        if (normalized.isBlank()) {
            throw new MaterialValidationException("Filename is not safe");
        }
        if (normalized.length() > MAX_FILENAME_LENGTH) {
            String extension = extractExtension(normalized);
            int suffixLength = extension.isBlank() ? 0 : extension.length() + 1;
            int baseLength = Math.max(1, MAX_FILENAME_LENGTH - suffixLength);
            String baseName = normalized.substring(0, Math.min(baseLength, normalized.lastIndexOf('.')));
            normalized = extension.isBlank() ? baseName : baseName + "." + extension;
        }
        return normalized;
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index <= 0 || index == filename.length() - 1) {
            throw new MaterialValidationException("File extension is required");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new MaterialValidationException("Content type is required");
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        byte[] buffer = new byte[(int) Math.min(HEADER_BYTES, file.getSize())];
        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.read(buffer);
            if (read <= 0) {
                throw new MaterialValidationException("File must not be empty");
            }
            return read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
        } catch (IOException ex) {
            throw new MaterialValidationException("Unable to read uploaded file", ex);
        }
    }

    private boolean signatureMatches(String extension, String contentType, byte[] header) {
        return switch (extension) {
            case "pdf" -> startsWith(header, "%PDF".getBytes());
            case "doc", "ppt", "xls" -> startsWith(header, bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1));
            case "docx", "pptx", "xlsx", "odt", "odp", "ods", "zip" -> startsWith(header, bytes(0x50, 0x4B, 0x03, 0x04))
                    || startsWith(header, bytes(0x50, 0x4B, 0x05, 0x06))
                    || startsWith(header, bytes(0x50, 0x4B, 0x07, 0x08));
            case "txt", "md", "markdown", "csv", "rtf" -> isPlainText(header);
            case "jpg", "jpeg" -> startsWith(header, bytes(0xFF, 0xD8, 0xFF));
            case "png" -> startsWith(header, bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A));
            case "gif" -> startsWith(header, "GIF87a".getBytes()) || startsWith(header, "GIF89a".getBytes());
            case "webp" -> startsWith(header, "RIFF".getBytes()) && containsAt(header, "WEBP".getBytes(), 8);
            case "mp4", "mov", "m4a" -> containsAt(header, "ftyp".getBytes(), 4);
            case "webm" -> startsWith(header, bytes(0x1A, 0x45, 0xDF, 0xA3));
            case "avi" -> startsWith(header, "RIFF".getBytes()) && containsAt(header, "AVI ".getBytes(), 8);
            case "mp3" -> startsWith(header, "ID3".getBytes()) || hasMp3FrameSync(header);
            case "wav" -> startsWith(header, "RIFF".getBytes()) && containsAt(header, "WAVE".getBytes(), 8);
            case "ogg" -> startsWith(header, "OggS".getBytes());
            case "7z" -> startsWith(header, bytes(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C));
            case "rar" -> startsWith(header, "Rar!".getBytes());
            case "svg" -> contentType.equals("image/svg+xml") && isPlainText(header);
            default -> contentType.startsWith("text/") && isPlainText(header);
        };
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAt(byte[] actual, byte[] expected, int offset) {
        if (actual.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (actual[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasMp3FrameSync(byte[] header) {
        return header.length >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
    }

    private boolean isPlainText(byte[] header) {
        int suspicious = 0;
        for (byte b : header) {
            int value = b & 0xFF;
            if (value == 0) {
                return false;
            }
            boolean allowedControl = value == '\n' || value == '\r' || value == '\t';
            if (value < 32 && !allowedControl) {
                suspicious++;
            }
        }
        return suspicious <= Math.max(1, header.length / 20);
    }

    private byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }
}
