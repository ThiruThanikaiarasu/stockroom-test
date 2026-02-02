package com.stockroom.core.purchasing;

import java.math.BigDecimal;

import com.stockroom.core.common.InvalidStateException;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", updatable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @Column(name = "ordered_qty", nullable = false)
    private int orderedQuantity;

    @Column(name = "received_qty", nullable = false)
    private int receivedQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    public PurchaseOrderLine(Sku sku, int orderedQuantity, BigDecimal unitCost) {
        this.sku = sku;
        this.orderedQuantity = orderedQuantity;
        this.unitCost = unitCost;
    }

    void attachTo(PurchaseOrder order) {
        this.purchaseOrder = order;
    }

    public int outstanding() {
        return orderedQuantity - receivedQuantity;
    }

    public boolean isFullyReceived() {
        return receivedQuantity >= orderedQuantity;
    }

    void receive(int quantity) {
        if (quantity > outstanding()) {
            throw new InvalidStateException(
                    "Cannot receive " + quantity + " of " + sku.getCode() + "; only " + outstanding() + " outstanding");
        }
        receivedQuantity += quantity;
    }
}
