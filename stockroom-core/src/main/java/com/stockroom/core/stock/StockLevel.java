package com.stockroom.core.stock;

import com.stockroom.core.location.Bin;
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

/** On-hand and reserved quantity of one SKU in one bin. */
@Entity
@Table(name = "stock_levels")
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id")
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bin_id")
    private Bin bin;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    protected StockLevel() {
    }

    public StockLevel(Sku sku, Bin bin) {
        this.sku = sku;
        this.bin = bin;
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

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public int available() {
        return onHand - reserved;
    }

    void add(int quantity) {
        onHand += quantity;
    }

    void remove(int quantity) {
        if (quantity > onHand - reserved) {
            throw new IllegalStateException("cannot remove more than the unreserved quantity");
        }
        onHand -= quantity;
    }

    public void reserve(int quantity) {
        reserved += quantity;
    }

    public void unreserve(int quantity) {
        reserved = Math.max(0, reserved - quantity);
    }

    void consumeReserved(int quantity) {
        reserved -= quantity;
        onHand -= quantity;
    }
}
