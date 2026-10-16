package com.mentify.service.registration;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates high-entropy temporary credentials without relying on java.util.Random. */
@Component
public class TemporaryPasswordGenerator {

    private static final int PASSWORD_LENGTH = 32;
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private final SecureRandom secureRandom;

    public TemporaryPasswordGenerator() {
        this(new SecureRandom());
    }

    TemporaryPasswordGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] password = new char[PASSWORD_LENGTH];
        password[0] = pick(UPPER);
        password[1] = pick(LOWER);
        password[2] = pick(DIGITS);
        password[3] = pick(SYMBOLS);
        for (int i = 4; i < password.length; i++) {
            password[i] = pick(ALL);
        }
        for (int i = password.length - 1; i > 0; i--) {
            int swapIndex = secureRandom.nextInt(i + 1);
            char current = password[i];
            password[i] = password[swapIndex];
            password[swapIndex] = current;
        }
        return new String(password);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(secureRandom.nextInt(alphabet.length()));
    }
}
