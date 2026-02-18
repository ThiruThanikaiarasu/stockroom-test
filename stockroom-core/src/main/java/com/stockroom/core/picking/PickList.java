package com.stockroom.core.picking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

@Entity
@Table(name = "pick_lists")
public class PickList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "order_ref", nullable = false, updatable = false)
    private String orderRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickListStatus status = PickListStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pickList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PickListLine> lines = new ArrayList<>();

    protected PickList() {
    }

    public PickList(String orderRef, LocalDateTime createdAt) {
        this.publicId = UUID.randomUUID();
        this.orderRef = orderRef;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public PickListStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PickListLine> getLines() {
        return lines;
    }

    void addLine(PickListLine line) {
        line.attachTo(this);
        lines.add(line);
    }

    void complete() {
        requireOpen();
        status = PickListStatus.COMPLETED;
    }

    void cancel() {
        requireOpen();
        status = PickListStatus.CANCELLED;
    }

    void requireOpen() {
        if (status != PickListStatus.OPEN) {
            throw new InvalidStateException("Pick list " + publicId + " is " + status);
        }
    }
}
