package com.example.hyperlocal.inventory.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.dto.InventoryDto;
import com.example.hyperlocal.inventory.dto.StockAdjustmentRequest;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.product.entity.Product;
import com.example.hyperlocal.product.repository.ProductRepository;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            ProductRepository productRepository,
            VendorRepository vendorRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getVendorInventory(Long ownerUserId) {
        Vendor vendor = getVendorByOwner(ownerUserId);
        List<Product> products = productRepository.findByVendorIdAndDeletedAtIsNull(vendor.getId());
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        return inventoryRepository.findByProductIdIn(productIds).stream()
                .map(inv -> InventoryDto.fromEntity(inv, productMap.get(inv.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getLowStockInventory(Long ownerUserId) {
        Vendor vendor = getVendorByOwner(ownerUserId);
        List<Product> products = productRepository.findByVendorIdAndDeletedAtIsNull(vendor.getId());
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        return inventoryRepository.findLowStockInventories(productIds).stream()
                .map(inv -> InventoryDto.fromEntity(inv, productMap.get(inv.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryDto adjustStock(Long ownerUserId, Long productId, StockAdjustmentRequest req) {
        Vendor vendor = getVendorByOwner(ownerUserId);
        Product product = productRepository.findByIdAndVendorIdAndDeletedAtIsNull(productId, vendor.getId())
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found in your store", HttpStatus.NOT_FOUND));

        // Use pessimistic lock for stock adjustment
        Inventory inventory = inventoryRepository.findWithLockByProductId(productId)
                .orElseGet(() -> new Inventory(productId, 0, 5));

        int newQty = inventory.getAvailableQuantity() + req.getQuantityChange();
        if (newQty < 0) {
            throw new ApiException("NEGATIVE_INVENTORY_PROHIBITED",
                    "Stock cannot be reduced below zero. Available: " + inventory.getAvailableQuantity() +
                    ", requested change: " + req.getQuantityChange(), HttpStatus.BAD_REQUEST);
        }

        inventory.setAvailableQuantity(newQty);
        Inventory saved = inventoryRepository.save(inventory);

        // Record movement audit
        InventoryMovement movement = new InventoryMovement(
                productId,
                req.getQuantityChange(),
                newQty,
                req.getReason() != null ? req.getReason() : "MANUAL_ADJUSTMENT",
                "MANUAL",
                UUID.randomUUID().toString().substring(0, 8),
                ownerUserId
        );
        movementRepository.save(movement);

        return InventoryDto.fromEntity(saved, product);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovement> getMovements(Long ownerUserId, Long productId, Pageable pageable) {
        Vendor vendor = getVendorByOwner(ownerUserId);
        productRepository.findByIdAndVendorIdAndDeletedAtIsNull(productId, vendor.getId())
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found in your store", HttpStatus.NOT_FOUND));

        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }

    private Vendor getVendorByOwner(Long ownerUserId) {
        return vendorRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "No store registered for this vendor account", HttpStatus.NOT_FOUND));
    }
}
