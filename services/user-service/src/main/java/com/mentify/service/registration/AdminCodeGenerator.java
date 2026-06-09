package com.mentify.service.registration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminCodeGenerator {

    public String generate() {
        return "ADM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
