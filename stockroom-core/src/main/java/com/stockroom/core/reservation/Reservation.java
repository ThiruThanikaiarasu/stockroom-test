package com.stockroom.core.reservation;

import java.time.LocalDateTime;

import com.stockroom.core.common.InvalidStateException;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String reference;

    @Column(name = "order_ref", nullable = false, updatable = false)
    private String orderRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bin_id", updatable = false)
    private Bin bin;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public Reservation(String reference, String orderRef, Sku sku, Bin bin, int quantity,
                       LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.reference = reference;
        this.orderRef = orderRef;
        this.sku = sku;
        this.bin = bin;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    void release() {
        requireActive();
        status = ReservationStatus.RELEASED;
    }

    void expire() {
        requireActive();
        status = ReservationStatus.EXPIRED;
    }

    public void fulfil() {
        requireActive();
        status = ReservationStatus.FULFILLED;
    }

    void extendTo(LocalDateTime newExpiry) {
        requireActive();
        expiresAt = newExpiry;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new InvalidStateException("Reservation " + reference + " is " + status);
        }
    }
}
