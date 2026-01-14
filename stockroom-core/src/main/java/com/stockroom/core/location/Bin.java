package com.stockroom.core.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A storage bin, addressed as {@code <aisle>-<bay>} within a location, e.g. {@code C-12}. */
@Entity
@Table(name = "bins")
public class Bin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, updatable = false)
    private String aisle;

    @Column(nullable = false, updatable = false)
    private int bay;

    @Column(nullable = false)
    private boolean pickable = true;

    protected Bin() {
    }

    public Bin(Location location, String aisle, int bay) {
        this.location = location;
        this.aisle = aisle;
        this.bay = bay;
        this.code = aisle + "-" + bay;
    }

    public Long getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getCode() {
        return code;
    }

    public String getAisle() {
        return aisle;
    }

    public int getBay() {
        return bay;
    }

    public boolean isPickable() {
        return pickable;
    }

    public void setPickable(boolean pickable) {
        this.pickable = pickable;
    }
}
