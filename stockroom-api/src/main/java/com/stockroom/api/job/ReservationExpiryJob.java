package com.stockroom.api.job;

import com.stockroom.core.reservation.ReservationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "stockroom.reservations", name = "sweep-enabled", havingValue = "true",
        matchIfMissing = true)
public class ReservationExpiryJob {

    private final ReservationService reservations;

    public ReservationExpiryJob(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${stockroom.reservations.sweep-interval:PT1M}",
            initialDelayString = "${stockroom.reservations.sweep-interval:PT1M}")
    public void sweep() {
        reservations.expireDue();
    }
}
