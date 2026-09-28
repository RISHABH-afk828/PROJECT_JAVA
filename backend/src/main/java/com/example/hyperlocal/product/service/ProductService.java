package com.example.hyperlocal.product.service;

import com.example.hyperlocal.category.entity.Category;
import com.example.hyperlocal.category.repository.CategoryRepository;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.dto.ProductRequest;
import com.example.hyperlocal.product.entity.Product;
import com.example.hyperlocal.product.repository.ProductRepository;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final VendorRepository vendorRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            VendorRepository vendorRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getVendorProducts(Long vendorId, Long categoryId, boolean activeOnly, String query, int page, int size) {
        Page<Product> productPage = productRepository.findVendorProducts(
                vendorId, categoryId, activeOnly, query, PageRequest.of(page, size));

        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));

        Map<Long, String> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<ProductDto> dtos = products.stream()
                .map(p -> {
                    Inventory inv = inventoryMap.get(p.getId());
                    String catName = categoryMap.getOrDefault(p.getCategoryId(), "General");
                    return ProductDto.fromEntity(
                            p,
                            catName,
                            inv != null ? inv.getAvailableQuantity() : 0,
                            inv != null ? inv.getLowStockThreshold() : 5
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, PageRequest.of(page, size), productPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        Inventory inv = inventoryRepository.findByProductId(productId).orElse(null);
        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse("General");

        return ProductDto.fromEntity(
                product,
                categoryName,
                inv != null ? inv.getAvailableQuantity() : 0,
                inv != null ? inv.getLowStockThreshold() : 5
        );
    }

    @Transactional
    public ProductDto createProduct(Long ownerUserId, Long vendorId, ProductRequest req) {
        validateVendorOwnership(ownerUserId, vendorId);

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.BAD_REQUEST));

        Product product = new Product(vendorId, category.getId(), req.getName().trim(), req.getPrice(), req.getUnit().trim());
        product.setDescription(req.getDescription());
        product.setSku(req.getSku());
        product.setImageUrl(req.getImageUrl());
        product.setSlug(req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        if (req.getActive() != null) {
            product.setActive(req.getActive());
        }

        Product savedProduct = productRepository.save(product);

        // Initialize Inventory
        int initialStock = req.getInitialStock() != null ? req.getInitialStock() : 0;
        int threshold = req.getLowStockThreshold() != null ? req.getLowStockThreshold() : 5;
        Inventory inventory = new Inventory(savedProduct.getId(), initialStock, threshold);
        inventoryRepository.save(inventory);

        if (initialStock > 0) {
            InventoryMovement movement = new InventoryMovement(
                    savedProduct.getId(),
                    initialStock,
                    initialStock,
                    "RESTOCK",
                    "INITIAL_STOCK",
                    "PRODUCT_CREATE",
                    ownerUserId
            );
            movementRepository.save(movement);
        }

        return ProductDto.fromEntity(savedProduct, category.getName(), initialStock, threshold);
    }

    @Transactional
    public ProductDto updateProduct(Long ownerUserId, Long vendorId, Long productId, ProductRequest req) {
        validateVendorOwnership(ownerUserId, vendorId);

        Product product = productRepository.findByIdAndVendorIdAndDeletedAtIsNull(productId, vendorId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found or does not belong to your store", HttpStatus.NOT_FOUND));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.BAD_REQUEST));

        product.setName(req.getName().trim());
        product.setCategoryId(category.getId());
        product.setPrice(req.getPrice());
        product.setUnit(req.getUnit().trim());
        product.setDescription(req.getDescription());
        product.setSku(req.getSku());
        if (req.getImageUrl() != null) {
            product.setImageUrl(req.getImageUrl());
        }
        if (req.getActive() != null) {
            product.setActive(req.getActive());
        }

        Product updatedProduct = productRepository.save(product);

        Inventory inv = inventoryRepository.findByProductId(productId).orElse(null);
        if (inv != null && req.getLowStockThreshold() != null) {
            inv.setLowStockThreshold(req.getLowStockThreshold());
            inventoryRepository.save(inv);
        }

        return ProductDto.fromEntity(
                updatedProduct,
                category.getName(),
                inv != null ? inv.getAvailableQuantity() : 0,
                inv != null ? inv.getLowStockThreshold() : 5
        );
    }

    @Transactional
    public void softDeleteProduct(Long ownerUserId, Long vendorId, Long productId) {
        validateVendorOwnership(ownerUserId, vendorId);
        Product product = productRepository.findByIdAndVendorIdAndDeletedAtIsNull(productId, vendorId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        product.setDeletedAt(Instant.now());
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void setProductStatus(Long ownerUserId, Long vendorId, Long productId, boolean active) {
        validateVendorOwnership(ownerUserId, vendorId);
        Product product = productRepository.findByIdAndVendorIdAndDeletedAtIsNull(productId, vendorId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        product.setActive(active);
        productRepository.save(product);
    }

    private void validateVendorOwnership(Long ownerUserId, Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        if (!vendor.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("ACCESS_DENIED", "You do not have permission to manage this store's catalog", HttpStatus.FORBIDDEN);
        }
    }
}
