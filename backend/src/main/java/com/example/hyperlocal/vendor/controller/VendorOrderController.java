package com.example.hyperlocal.vendor.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.vendor.service.VendorOrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendor/orders")
@PreAuthorize("hasRole('VENDOR')")
public class VendorOrderController {

    private final VendorOrderService vendorOrderService;

    public VendorOrderController(VendorOrderService vendorOrderService) {
        this.vendorOrderService = vendorOrderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getVendorOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderDto> orders = vendorOrderService.getVendorOrders(principal.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getVendorOrderDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = vendorOrderService.getVendorOrderDetails(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<OrderDto>> acceptOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = vendorOrderService.acceptOrder(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{id}/preparing")
    public ResponseEntity<ApiResponse<OrderDto>> startPreparingOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = vendorOrderService.startPreparingOrder(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<OrderDto>> markOrderReady(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = vendorOrderService.markOrderReady(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OrderDto>> rejectOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        OrderDto order = vendorOrderService.rejectOrder(principal.getId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
