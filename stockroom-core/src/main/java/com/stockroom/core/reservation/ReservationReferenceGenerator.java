package com.stockroom.core.reservation;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ReservationReferenceGenerator {

    public String next() {
        return "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
