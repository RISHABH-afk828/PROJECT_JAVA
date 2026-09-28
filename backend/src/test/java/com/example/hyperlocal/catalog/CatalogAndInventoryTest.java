package com.example.hyperlocal.catalog;

import com.example.hyperlocal.category.dto.CategoryDto;
import com.example.hyperlocal.category.service.CategoryService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.dto.InventoryDto;
import com.example.hyperlocal.inventory.dto.StockAdjustmentRequest;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.service.InventoryService;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.dto.ProductRequest;
import com.example.hyperlocal.product.service.ProductService;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class CatalogAndInventoryTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private VendorService vendorService;

    @Test
    void testCatalogCreationAndInventoryAdjustment() {
        // Find existing seed vendor (Fresh Mart, owner: vOwner1)
        List<VendorDto> vendors = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent();
        assertFalse(vendors.isEmpty());
        VendorDto vendor = vendors.get(0);

        List<CategoryDto> categories = categoryService.getActiveCategories();
        assertFalse(categories.isEmpty());
        CategoryDto category = categories.get(0);

        // 1. Create a new product
        ProductRequest req = new ProductRequest();
        req.setName("Test Organic Butter Cookies");
        req.setCategoryId(category.getId());
        req.setPrice(new BigDecimal("120.00"));
        req.setUnit("200g box");
        req.setDescription("Crispy rich butter cookies made with real butter");
        req.setInitialStock(15);
        req.setLowStockThreshold(5);
        req.setActive(true);

        ProductDto created = productService.createProduct(vendor.getOwnerUserId(), vendor.getId(), req);
        assertNotNull(created.getId());
        assertEquals("Test Organic Butter Cookies", created.getName());
        assertEquals(15, created.getAvailableQuantity());
        assertFalse(created.getIsLowStock());

        // 2. Adjust Stock (+10 RESTOCK)
        StockAdjustmentRequest restock = new StockAdjustmentRequest();
        restock.setQuantityChange(10);
        restock.setReason("RESTOCK");

        InventoryDto updatedInv = inventoryService.adjustStock(vendor.getOwnerUserId(), created.getId(), restock);
        assertEquals(25, updatedInv.getAvailableQuantity());

        // Verify movement log
        Page<InventoryMovement> movements = inventoryService.getMovements(
                vendor.getOwnerUserId(), created.getId(), PageRequest.of(0, 10));
        assertFalse(movements.isEmpty());
        assertEquals("RESTOCK", movements.getContent().get(0).getReason());

        // 3. Adjust Stock into Low Stock condition (-22 adjustment -> leaves 3, threshold is 5)
        StockAdjustmentRequest reduce = new StockAdjustmentRequest();
        reduce.setQuantityChange(-22);
        reduce.setReason("MANUAL_ADJUSTMENT");

        InventoryDto lowInv = inventoryService.adjustStock(vendor.getOwnerUserId(), created.getId(), reduce);
        assertEquals(3, lowInv.getAvailableQuantity());
        assertTrue(lowInv.getIsLowStock(), "Product stock is 3 <= threshold 5, should be flagged as low stock");

        // 4. Test Negative Stock Prevention (Attempt to deduct 10 when only 3 available)
        StockAdjustmentRequest overDeduct = new StockAdjustmentRequest();
        overDeduct.setQuantityChange(-10);
        overDeduct.setReason("MANUAL_ADJUSTMENT");

        ApiException negEx = assertThrows(ApiException.class,
                () -> inventoryService.adjustStock(vendor.getOwnerUserId(), created.getId(), overDeduct));
        assertEquals("NEGATIVE_INVENTORY_PROHIBITED", negEx.getCode());

        // 5. Test Soft Delete
        productService.softDeleteProduct(vendor.getOwnerUserId(), vendor.getId(), created.getId());
        assertThrows(ApiException.class, () -> productService.getProductById(created.getId()),
                "Soft deleted product should no longer be returned by getProductById");
    }
}
