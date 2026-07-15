package com.mentify.service.impl;

import com.mentify.service.StudentValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceStudentValidationService implements StudentValidationService {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.user-service.base-url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @Override
    public boolean studentExists(UUID studentId) {
        String uri = UriComponentsBuilder
                .fromHttpUrl(userServiceBaseUrl)
                .path("/api/v1/users/{studentId}")
                .buildAndExpand(studentId)
                .toUriString();

        try {
            restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to validate student details", ex);
        }
    }
}
