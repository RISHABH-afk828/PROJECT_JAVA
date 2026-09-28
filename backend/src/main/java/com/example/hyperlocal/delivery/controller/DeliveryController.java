package com.example.hyperlocal.delivery.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.delivery.dto.DeliveryLocationUpdateRequest;
import com.example.hyperlocal.delivery.dto.DeliveryProfileDto;
import com.example.hyperlocal.delivery.dto.DeliveryStatusUpdateRequest;
import com.example.hyperlocal.delivery.service.DeliveryService;
import com.example.hyperlocal.order.dto.OrderDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery")
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DeliveryProfileDto>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        DeliveryProfileDto profile = deliveryService.getOrCreateProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<DeliveryProfileDto>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryStatusUpdateRequest req) {
        DeliveryProfileDto profile = deliveryService.updateOnlineStatus(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<DeliveryProfileDto>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryLocationUpdateRequest req) {
        DeliveryProfileDto profile = deliveryService.updateLocation(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/available-orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAvailableOrders(@AuthenticationPrincipal UserPrincipal principal) {
        List<OrderDto> orders = deliveryService.getAvailableOrders(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderDto> orders = deliveryService.getMyOrders(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<ApiResponse<OrderDto>> acceptOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = deliveryService.acceptOrder(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/orders/{id}/pickup")
    public ResponseEntity<ApiResponse<OrderDto>> confirmPickup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = deliveryService.confirmPickup(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/orders/{id}/out-for-delivery")
    public ResponseEntity<ApiResponse<OrderDto>> startOutForDelivery(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = deliveryService.startOutForDelivery(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderDto>> completeDelivery(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = deliveryService.completeDelivery(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
