package com.mentify.assignment.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long fileSize;
    private Path targetPath;
}
