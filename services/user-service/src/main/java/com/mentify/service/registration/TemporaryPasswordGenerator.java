package com.mentify.service.registration;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates unpredictable temporary credentials without relying on java.util.Random. */
@Component
public class TemporaryPasswordGenerator {

    private static final int PASSWORD_LENGTH = 8;
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*_-+=";
    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS;

    private final SecureRandom secureRandom;

    public TemporaryPasswordGenerator() {
        this(new SecureRandom());
    }

    TemporaryPasswordGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        char[] password = new char[PASSWORD_LENGTH];
        password[0] = randomCharacter(UPPERCASE);
        password[1] = randomCharacter(LOWERCASE);
        password[2] = randomCharacter(DIGITS);
        password[3] = randomCharacter(SYMBOLS);
        for (int i = 4; i < password.length; i++) {
            password[i] = randomCharacter(ALL_CHARACTERS);
        }

        // Shuffle the required character classes so the first four positions reveal no pattern.
        for (int index = password.length - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            char temporary = password[index];
            password[index] = password[swapIndex];
            password[swapIndex] = temporary;
        }
        return new String(password);
    }

    private char randomCharacter(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }
}
