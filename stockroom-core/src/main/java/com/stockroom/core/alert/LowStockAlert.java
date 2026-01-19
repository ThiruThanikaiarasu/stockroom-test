package com.stockroom.core.alert;

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
@Table(name = "low_stock_alerts")
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @Column(nullable = false, updatable = false)
    private int available;

    @Column(name = "reorder_point", nullable = false, updatable = false)
    private int reorderPoint;

    @Column(name = "raised_at", nullable = false, updatable = false)
    private LocalDateTime raisedAt;

    @Column(nullable = false)
    private boolean acknowledged;

    protected LowStockAlert() {
    }

    public LowStockAlert(Sku sku, int available, int reorderPoint, LocalDateTime raisedAt) {
        this.sku = sku;
        this.available = available;
        this.reorderPoint = reorderPoint;
        this.raisedAt = raisedAt;
    }

    public Long getId() {
        return id;
    }

    public Sku getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void acknowledge() {
        this.acknowledged = true;
    }
}
