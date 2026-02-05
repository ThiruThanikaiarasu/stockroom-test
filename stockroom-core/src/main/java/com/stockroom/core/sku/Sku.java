package com.stockroom.core.sku;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "skus")
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    /** Minimum level; a low-stock alert is raised once available stock drops below it. */
    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Sku(String code, String name, String unitOfMeasure, int reorderPoint, LocalDateTime createdAt) {
        this.code = code;
        this.name = name;
        this.unitOfMeasure = unitOfMeasure;
        this.reorderPoint = reorderPoint;
        this.createdAt = createdAt;
    }
}
