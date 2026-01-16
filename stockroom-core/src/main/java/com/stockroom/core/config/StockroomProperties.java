package com.stockroom.core.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockroom")
public record StockroomProperties(Reservations reservations, Alerts alerts) {

    public StockroomProperties {
        if (reservations == null) {
            reservations = new Reservations(null);
        }
        if (alerts == null) {
            alerts = new Alerts(true);
        }
    }

    public record Reservations(Duration ttl) {
        public Reservations {
            if (ttl == null) {
                ttl = Duration.ofMinutes(30);
            }
        }
    }

    public record Alerts(boolean enabled) {
    }
}
