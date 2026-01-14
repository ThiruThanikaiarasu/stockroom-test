package com.stockroom.core.stock;

import java.time.LocalDateTime;

import com.stockroom.core.location.Bin;
import com.stockroom.core.sku.Sku;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Immutable ledger entry. Quantity is signed: positive into the bin, negative out of it. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bin_id", updatable = false)
    private Bin bin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MovementType type;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(updatable = false)
    private String reference;

    @Column(name = "transfer_id", updatable = false)
    private String transferId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected StockMovement() {
    }

    public StockMovement(Sku sku, Bin bin, MovementType type, int quantity, String reference,
                         String transferId, LocalDateTime occurredAt) {
        this.sku = sku;
        this.bin = bin;
        this.type = type;
        this.quantity = quantity;
        this.reference = reference;
        this.transferId = transferId;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Sku getSku() {
        return sku;
    }

    public Bin getBin() {
        return bin;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReference() {
        return reference;
    }

    public String getTransferId() {
        return transferId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
