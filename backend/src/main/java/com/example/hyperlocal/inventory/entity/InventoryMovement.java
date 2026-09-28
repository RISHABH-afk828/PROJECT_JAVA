package com.example.hyperlocal.inventory.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements", indexes = {
        @Index(name = "idx_movement_product", columnList = "product_id"),
        @Index(name = "idx_movement_created", columnList = "created_at")
})
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange; // e.g. +20, -2

    @Column(name = "resulting_quantity", nullable = false)
    private Integer resultingQuantity;

    @Column(nullable = false, length = 50)
    private String reason; // RESTOCK, ORDER, MANUAL_ADJUSTMENT, CANCEL_RESTORE

    @Column(name = "reference_type", length = 50)
    private String referenceType; // ORDER, ADJUSTMENT

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InventoryMovement() {
    }

    public InventoryMovement(Long productId, Integer quantityChange, Integer resultingQuantity, String reason, String referenceType, String referenceId, Long createdBy) {
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.resultingQuantity = resultingQuantity;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getResultingQuantity() {
        return resultingQuantity;
    }

    public void setResultingQuantity(Integer resultingQuantity) {
        this.resultingQuantity = resultingQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
