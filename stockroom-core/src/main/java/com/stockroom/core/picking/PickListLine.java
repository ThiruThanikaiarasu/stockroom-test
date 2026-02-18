package com.stockroom.core.picking;

import com.stockroom.core.location.Bin;
import com.stockroom.core.reservation.Reservation;
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
@Table(name = "pick_list_lines")
public class PickListLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pick_list_id", updatable = false)
    private PickList pickList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", updatable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bin_id", updatable = false)
    private Bin bin;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(nullable = false)
    private int picked;

    protected PickListLine() {
    }

    public PickListLine(Reservation reservation) {
        this.reservation = reservation;
        this.sku = reservation.getSku();
        this.bin = reservation.getBin();
        this.quantity = reservation.getQuantity();
    }

    void attachTo(PickList pickList) {
        this.pickList = pickList;
    }

    public Long getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Sku getSku() {
        return sku;
    }

    public Bin getBin() {
        return bin;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPicked() {
        return picked;
    }

    void markPicked() {
        this.picked = quantity;
    }
}
