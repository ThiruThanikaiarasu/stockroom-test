package com.stockroom.core.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ReservationReferenceGeneratorTest {

    private final ReservationReferenceGenerator generator = new ReservationReferenceGenerator();

    @Test
    void referencesHaveTheDocumentedShape() {
        assertThat(generator.next()).matches("RSV-[0-9A-F]{8}");
    }

    @Test
    void referencesDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(generator.next());
        }
        assertThat(seen).hasSize(10_000);
    }
}
