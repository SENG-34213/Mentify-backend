package com.mentify.service.registration;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates unpredictable numeric temporary credentials without relying on java.util.Random. */
@Component
public class TemporaryPasswordGenerator {

    private static final int PASSWORD_LENGTH = 8;
    private static final int DIGIT_BOUND = 10;

    private final SecureRandom secureRandom;

    public TemporaryPasswordGenerator() {
        this(new SecureRandom());
    }

    TemporaryPasswordGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] password = new char[PASSWORD_LENGTH];
        for (int i = 0; i < password.length; i++) {
            password[i] = (char) ('0' + secureRandom.nextInt(DIGIT_BOUND));
        }
        return new String(password);
    }
}
