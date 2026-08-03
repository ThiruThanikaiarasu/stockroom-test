package com.stockroom.core.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.stockroom.StockroomTestApplication;
import com.stockroom.core.common.InsufficientStockException;
import com.stockroom.core.common.InvalidStateException;
import com.stockroom.core.location.Bin;
import com.stockroom.core.location.Location;
import com.stockroom.core.location.LocationService;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.stock.StockService;
import com.stockroom.testsupport.Fixtures;
import com.stockroom.testsupport.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReservationServiceTest {

    @Autowired
    ReservationService reservations;

    @Autowired
    StockService stock;

    @Autowired
    LocationService locations;

    @Autowired
    Fixtures fixtures;

    @Autowired
    MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.set(StockroomTestApplication.START);
    }

    @Test
    void reservingHoldsStockForThirtyMinutes() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 10);

        Reservation reservation = reservations.reserve("ORD-100", sku.getId(), 3);

        assertThat(reservation.getReference()).startsWith("RSV-");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(Duration.between(reservation.getCreatedAt(), reservation.getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(stock.available(sku.getId())).isEqualTo(7);
        assertThat(stock.onHand(sku.getId())).isEqualTo(10);
    }

    @Test
    void cannotReserveMoreThanIsAvailable() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 2);

        assertThatThrownBy(() -> reservations.reserve("ORD-101", sku.getId(), 3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 2");
    }

    @Test
    void reservationsAreNeverSplitAcrossBins() {
        Sku sku = fixtures.sku(0);
        Location location = fixtures.location();
        Bin small = fixtures.bin(location, "A", 1);
        Bin large = fixtures.bin(location, "B", 1);
        stock.receive(sku.getId(), small.getId(), 2, "PO");
        stock.receive(sku.getId(), large.getId(), 6, "PO");

        Reservation reservation = reservations.reserve("ORD-102", sku.getId(), 5);

        assertThat(reservation.getBin().getId()).isEqualTo(large.getId());
    }

    @Test
    void nonPickableBinsAreSkipped() {
        Sku sku = fixtures.sku(0);
        Location location = fixtures.location();
        Bin quarantine = fixtures.bin(location, "Q", 1);
        stock.receive(sku.getId(), quarantine.getId(), 10, "PO");
        locations.setPickable(quarantine.getId(), false);
        fixtures.bin(location, "A", 1);

        assertThatThrownBy(() -> reservations.reserve("ORD-103", sku.getId(), 1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void releasingReturnsStock() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 5);
        Reservation reservation = reservations.reserve("ORD-104", sku.getId(), 5);

        reservations.release(reservation.getReference());

        assertThat(reservations.get(reservation.getReference()).getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(stock.available(sku.getId())).isEqualTo(5);
        assertThatThrownBy(() -> reservations.release(reservation.getReference()))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void expiredReservationsReturnTheirStock() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 4);
        Reservation reservation = reservations.reserve("ORD-105", sku.getId(), 4);

        clock.advance(Duration.ofMinutes(45));
        reservations.expireDue();

        assertThat(reservations.get(reservation.getReference()).getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(stock.available(sku.getId())).isEqualTo(4);
    }

    @Test
    void extendingRestartsTheHoldFromNow() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 4);
        Reservation reservation = reservations.reserve("ORD-106", sku.getId(), 1);

        clock.advance(Duration.ofMinutes(20));
        Reservation extended = reservations.extend(reservation.getReference());

        assertThat(extended.getExpiresAt()).isEqualTo(reservation.getCreatedAt().plusMinutes(50));
    }

    @Test
    void activeReservationsAreListedPerOrder() {
        Sku sku = fixtures.sku(0);
        fixtures.stocked(sku, 9);
        String order = Fixtures.unique("ORD");
        reservations.reserve(order, sku.getId(), 1);
        reservations.reserve(order, sku.getId(), 2);
        Reservation released = reservations.reserve(order, sku.getId(), 3);
        reservations.release(released.getReference());

        assertThat(reservations.activeForOrder(order)).extracting(Reservation::getQuantity)
                .containsExactlyInAnyOrder(1, 2);
    }
}
