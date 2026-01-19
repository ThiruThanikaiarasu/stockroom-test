package com.stockroom.core.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.event.LowStockAlertRaised;
import com.stockroom.core.location.Bin;
import com.stockroom.core.reservation.ReservationService;
import com.stockroom.core.sku.Sku;
import com.stockroom.core.stock.StockService;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LowStockMonitorTest {

    @Autowired
    StockService stock;

    @Autowired
    ReservationService reservations;

    @Autowired
    AlertService alertService;

    @Autowired
    LowStockAlertRepository alerts;

    @Autowired
    DomainEventBus events;

    @Autowired
    Fixtures fixtures;

    private final List<LowStockAlertRaised> raised = new ArrayList<>();
    private DomainEventBus.Subscription subscription;

    @BeforeEach
    void listen() {
        subscription = events.subscribe(LowStockAlertRaised.class, raised::add);
    }

    @AfterEach
    void stopListening() {
        subscription.cancel();
    }

    @Test
    void raisesAnAlertWhenStockFallsWellBelowTheReorderPoint() {
        Sku sku = fixtures.sku(20);
        Bin bin = fixtures.stocked(sku, 50);

        stock.adjust(sku.getId(), bin.getId(), -45, "cycle count");

        assertThat(alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId())).singleElement().satisfies(alert -> {
            assertThat(alert.getAvailable()).isEqualTo(5);
            assertThat(alert.getReorderPoint()).isEqualTo(20);
        });
        assertThat(raised).extracting(LowStockAlertRaised::skuCode).containsExactly(sku.getCode());
    }

    @Test
    void reservationsCountAgainstAvailability() {
        Sku sku = fixtures.sku(10);
        fixtures.stocked(sku, 30);

        reservations.reserve(Fixtures.unique("ORD"), sku.getId(), 25);

        assertThat(alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId())).hasSize(1);
    }

    @Test
    void keepsOneOpenAlertPerSku() {
        Sku sku = fixtures.sku(20);
        Bin bin = fixtures.stocked(sku, 50);

        stock.adjust(sku.getId(), bin.getId(), -40, "count");
        stock.adjust(sku.getId(), bin.getId(), -5, "count");
        stock.adjust(sku.getId(), bin.getId(), -3, "count");

        assertThat(alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId())).hasSize(1);
    }

    @Test
    void acknowledgingAllowsTheNextDropToAlertAgain() {
        Sku sku = fixtures.sku(20);
        Bin bin = fixtures.stocked(sku, 50);
        stock.adjust(sku.getId(), bin.getId(), -40, "count");
        LowStockAlert first = alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId()).get(0);

        alertService.acknowledge(first.getId());
        stock.adjust(sku.getId(), bin.getId(), -5, "count");

        assertThat(alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId())).hasSize(2);
    }

    @Test
    void healthyStockRaisesNothing() {
        Sku sku = fixtures.sku(5);
        Bin bin = fixtures.stocked(sku, 100);

        stock.adjust(sku.getId(), bin.getId(), -50, "count");

        assertThat(alerts.findBySkuIdOrderByRaisedAtAsc(sku.getId())).isEmpty();
    }
}
