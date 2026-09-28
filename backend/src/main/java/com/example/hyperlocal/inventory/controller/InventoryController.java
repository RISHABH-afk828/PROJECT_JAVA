package com.example.hyperlocal.inventory.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.inventory.dto.InventoryDto;
import com.example.hyperlocal.inventory.dto.StockAdjustmentRequest;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendors/me/inventory")
@PreAuthorize("hasRole('VENDOR')")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getMyInventory(@AuthenticationPrincipal UserPrincipal principal) {
        List<InventoryDto> inventory = inventoryService.getVendorInventory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getLowStockInventory(@AuthenticationPrincipal UserPrincipal principal) {
        List<InventoryDto> lowStock = inventoryService.getLowStockInventory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(lowStock));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryDto>> adjustStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest req) {
        InventoryDto updated = inventoryService.adjustStock(principal.getId(), productId, req);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<ApiResponse<List<InventoryMovement>>> getMovements(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryMovement> movementPage = inventoryService.getMovements(principal.getId(), productId, PageRequest.of(page, size));
        Map<String, Object> meta = Map.of(
                "page", movementPage.getNumber(),
                "size", movementPage.getSize(),
                "totalElements", movementPage.getTotalElements(),
                "totalPages", movementPage.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(movementPage.getContent(), meta));
    }
}
