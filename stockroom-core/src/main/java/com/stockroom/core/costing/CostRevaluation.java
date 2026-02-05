package com.stockroom.core.costing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockroom.core.sku.Sku;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cost_revaluations")
public class CostRevaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @Column(name = "previous_cost", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal previousCost;

    @Column(name = "new_cost", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal newCost;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected CostRevaluation() {
    }

    public CostRevaluation(Sku sku, BigDecimal previousCost, BigDecimal newCost, LocalDateTime occurredAt) {
        this.sku = sku;
        this.previousCost = previousCost;
        this.newCost = newCost;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Sku getSku() {
        return sku;
    }

    public BigDecimal getPreviousCost() {
        return previousCost;
    }

    public BigDecimal getNewCost() {
        return newCost;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
