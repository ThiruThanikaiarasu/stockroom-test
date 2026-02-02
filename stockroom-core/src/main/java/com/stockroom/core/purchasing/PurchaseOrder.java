package com.stockroom.core.purchasing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.stockroom.core.common.InvalidStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String number;

    @Column(nullable = false)
    private String supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public PurchaseOrder(String number, String supplier, LocalDateTime createdAt) {
        this.number = number;
        this.supplier = supplier;
        this.createdAt = createdAt;
    }

    void addLine(PurchaseOrderLine line) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateException("Lines can only be added to a draft purchase order");
        }
        line.attachTo(this);
        lines.add(line);
    }

    void submit() {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStateException("Purchase order " + number + " is " + status);
        }
        if (lines.isEmpty()) {
            throw new InvalidStateException("Purchase order " + number + " has no lines");
        }
        status = PurchaseOrderStatus.SUBMITTED;
    }

    void cancel() {
        if (status != PurchaseOrderStatus.DRAFT && status != PurchaseOrderStatus.SUBMITTED) {
            throw new InvalidStateException("Purchase order " + number + " is " + status);
        }
        status = PurchaseOrderStatus.CANCELLED;
    }

    void requireReceivable() {
        if (status != PurchaseOrderStatus.SUBMITTED && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new InvalidStateException("Purchase order " + number + " is " + status);
        }
    }

    void refreshStatus() {
        boolean complete = lines.stream().allMatch(PurchaseOrderLine::isFullyReceived);
        boolean started = lines.stream().anyMatch(line -> line.getReceivedQuantity() > 0);
        if (complete) {
            status = PurchaseOrderStatus.RECEIVED;
        } else if (started) {
            status = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        }
    }
}
