package com.mentify.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "mentifyDotenv";
    private static final String DOTENV_FILE_NAME = ".env";
    private static final int MAX_PARENT_SEARCH_DEPTH = 6;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        findDotenv(Path.of(System.getProperty("user.dir")))
                .map(this::loadDotenv)
                .filter(properties -> !properties.isEmpty())
                .ifPresent(properties -> environment.getPropertySources().addLast(
                        new MapPropertySource(PROPERTY_SOURCE_NAME, properties)
                ));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

    private java.util.Optional<Path> findDotenv(Path startDirectory) {
        Path current = startDirectory.toAbsolutePath().normalize();

        for (int depth = 0; current != null && depth <= MAX_PARENT_SEARCH_DEPTH; depth++) {
            Path candidate = current.resolve(DOTENV_FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return java.util.Optional.of(candidate);
            }
            current = current.getParent();
        }

        return java.util.Optional.empty();
    }

    private Map<String, Object> loadDotenv(Path dotenvPath) {
        Map<String, Object> properties = new LinkedHashMap<>();

        try {
            List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                parseLine(line).ifPresent(entry -> properties.put(entry.key(), entry.value()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read local .env file: " + dotenvPath, exception);
        }

        return properties;
    }

    private java.util.Optional<DotenvEntry> parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return java.util.Optional.empty();
        }

        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }

        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return java.util.Optional.empty();
        }

        String key = trimmed.substring(0, separatorIndex).trim();
        String value = trimmed.substring(separatorIndex + 1).trim();

        if (key.isEmpty()) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new DotenvEntry(key, stripMatchingQuotes(value)));
    }

    private String stripMatchingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private record DotenvEntry(String key, String value) {
    }
}
