package com.example.hyperlocal.inventory.dto;

import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.product.entity.Product;

import java.time.Instant;

public class InventoryDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productUnit;
    private Integer availableQuantity;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Instant updatedAt;

    public static InventoryDto fromEntity(Inventory inventory, Product product) {
        InventoryDto dto = new InventoryDto();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setProductName(product != null ? product.getName() : "Unknown");
        dto.setProductUnit(product != null ? product.getUnit() : "");
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setLowStockThreshold(inventory.getLowStockThreshold());
        dto.setIsLowStock(inventory.isLowStock() && inventory.getAvailableQuantity() > 0);
        dto.setIsOutOfStock(inventory.isOutOfStock());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Boolean getIsLowStock() {
        return isLowStock;
    }

    public void setIsLowStock(Boolean lowStock) {
        isLowStock = lowStock;
    }

    public Boolean getIsOutOfStock() {
        return isOutOfStock;
    }

    public void setIsOutOfStock(Boolean outOfStock) {
        isOutOfStock = outOfStock;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
