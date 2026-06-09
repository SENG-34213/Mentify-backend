package com.mentify.service.registration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TeacherCodeGenerator {

    public String generate() {
        return "TCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
