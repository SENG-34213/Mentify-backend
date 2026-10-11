package com.mentify.ai.service.impl;

import com.mentify.ai.exception.AiInvalidDocumentException;
import com.mentify.ai.service.MaterialDownloadClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpMaterialDownloadClient implements MaterialDownloadClient {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public byte[] download(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new AiInvalidDocumentException("Material download URL is not available");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiInvalidDocumentException("Material file could not be downloaded");
            }
            return response.body();
        } catch (IOException ex) {
            throw new AiInvalidDocumentException("Material file could not be downloaded", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiInvalidDocumentException("Material file download was interrupted", ex);
        } catch (IllegalArgumentException ex) {
            throw new AiInvalidDocumentException("Material download URL is invalid", ex);
        }
    }
}
