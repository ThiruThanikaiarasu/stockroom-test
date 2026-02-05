package com.stockroom.core.costing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.stockroom.core.event.CostRevaluedEvent;
import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.sku.Sku;
import org.junit.jupiter.api.Test;

class CostingServiceTest {

    private final CostRevaluationRepository revaluations = mock(CostRevaluationRepository.class);
    private final DomainEventBus events = mock(DomainEventBus.class);
    private final CostingService costing = new CostingService(revaluations, events,
            Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC));

    @Test
    void firstReceiptSetsTheCost() {
        Sku sku = sku(BigDecimal.ZERO);

        BigDecimal cost = costing.applyReceipt(sku, 0, 10, new BigDecimal("4.2500"));

        assertThat(cost).isEqualByComparingTo("4.25");
        verify(revaluations).save(any(CostRevaluation.class));
        verify(events).publish(any(CostRevaluedEvent.class));
    }

    @Test
    void blendsWithExistingStockByQuantity() {
        Sku sku = sku(new BigDecimal("10.0000"));

        BigDecimal cost = costing.applyReceipt(sku, 30, 10, new BigDecimal("14.0000"));

        assertThat(cost).isEqualByComparingTo("11.00");
    }

    @Test
    void roundsTheAverageToFourDecimals() {
        assertThat(CostingService.weightedAverage(new BigDecimal("1.0000"), 2, new BigDecimal("2.0000"), 1))
                .isEqualByComparingTo("1.3333");
    }

    @Test
    void identicalCostKeepsAverage() {
        Sku sku = sku(new BigDecimal("3.5000"));

        costing.applyReceipt(sku, 12, 4, new BigDecimal("3.5000"));

        verify(revaluations, never()).save(any());
    }

    @Test
    void rejectsNegativeCost() {
        assertThatThrownBy(() -> costing.applyReceipt(sku(BigDecimal.ONE), 1, 1, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyReceipt() {
        assertThatThrownBy(() -> costing.applyReceipt(sku(BigDecimal.ONE), 1, 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Sku sku(BigDecimal averageCost) {
        Sku sku = new Sku("SKU-C", "Costed", "EA", 0, LocalDateTime.of(2026, 1, 1, 0, 0));
        sku.setAverageCost(averageCost);
        return sku;
    }
}
