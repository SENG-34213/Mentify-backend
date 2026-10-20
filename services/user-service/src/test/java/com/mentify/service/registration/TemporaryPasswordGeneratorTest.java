package com.mentify.service.registration;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @Test
    void generate_returnsEightCharacterPasswordWithAllRequiredCharacterClasses() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String password = generator.generate();
            assertThat(password)
                    .hasSize(8)
                    .matches("(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8}");
            generated.add(password);
        }
        assertThat(generated).hasSize(100);
    }
}
