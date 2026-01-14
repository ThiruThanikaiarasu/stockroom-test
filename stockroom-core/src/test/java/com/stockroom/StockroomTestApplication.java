package com.stockroom;

import java.time.Instant;

import com.stockroom.testsupport.MutableClock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class StockroomTestApplication {

    public static final Instant START = Instant.parse("2026-03-02T09:00:00Z");

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(START);
    }
}
