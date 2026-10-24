package com.mentify.service.registration;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @Test
    void generate_returnsEightCharacterMixedPassword() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String password = generator.generate();
            assertThat(password).hasSize(8);
            assertThat(password).matches(".*[A-Z].*");
            assertThat(password).matches(".*[a-z].*");
            assertThat(password).matches(".*[0-9].*");
            assertThat(password).matches(".*[^A-Za-z0-9].*");
            generated.add(password);
        }
        assertThat(generated).hasSize(100);
    }
}
