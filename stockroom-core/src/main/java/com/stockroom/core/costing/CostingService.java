package com.stockroom.core.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

import com.stockroom.core.event.CostRevaluedEvent;
import com.stockroom.core.event.DomainEventBus;
import com.stockroom.core.sku.Sku;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains each SKU's weighted average unit cost. */
@Service
@Transactional
public class CostingService {

    static final int COST_SCALE = 4;

    private final CostRevaluationRepository revaluations;
    private final DomainEventBus events;
    private final Clock clock;

    public CostingService(CostRevaluationRepository revaluations, DomainEventBus events, Clock clock) {
        this.revaluations = revaluations;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Blends a receipt into the SKU's average cost and records a revaluation when the average
     * changes.
     *
     * @param onHandBefore units on hand across all bins before the receipt
     * @return the SKU's average cost after the receipt
     */
    public BigDecimal applyReceipt(Sku sku, int onHandBefore, int receivedQuantity, BigDecimal unitCost) {
        if (receivedQuantity <= 0) {
            throw new IllegalArgumentException("receivedQuantity must be positive");
        }
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("unitCost must not be negative");
        }
        BigDecimal current = sku.getAverageCost();
        BigDecimal updated = weightedAverage(current, Math.max(onHandBefore, 0), unitCost, receivedQuantity);
        if (updated.compareTo(current) != 0) {
            sku.setAverageCost(updated);
            revaluations.save(new CostRevaluation(sku, current, updated, LocalDateTime.now(clock)));
            events.publish(new CostRevaluedEvent(sku.getId(), current, updated));
        }
        return sku.getAverageCost();
    }

    static BigDecimal weightedAverage(BigDecimal currentCost, int currentQuantity, BigDecimal unitCost,
                                      int receivedQuantity) {
        BigDecimal value = currentCost.multiply(BigDecimal.valueOf(currentQuantity))
                .add(unitCost.multiply(BigDecimal.valueOf(receivedQuantity)));
        return value.divide(BigDecimal.valueOf((long) currentQuantity + receivedQuantity), COST_SCALE,
                RoundingMode.HALF_UP);
    }
}
