package com.example.hyperlocal.order.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.dto.OrderStatusHistoryDto;
import com.example.hyperlocal.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final com.example.hyperlocal.delivery.service.DeliveryService deliveryService;

    public OrderController(OrderService orderService, com.example.hyperlocal.delivery.service.DeliveryService deliveryService) {
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderDto> orders = orderService.getCustomerOrders(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = orderService.getOrderById(id, principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderByNumber(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderNumber) {
        OrderDto order = orderService.getOrderByNumber(orderNumber, principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderTracking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        OrderDto order = orderService.getOrderById(id, principal.getId(), principal.getRole());
        List<OrderStatusHistoryDto> history = orderService.getOrderStatusHistory(id, principal.getId(), principal.getRole());

        Map<String, Object> tracking = new HashMap<>();
        tracking.put("order", order);
        tracking.put("history", history);

        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    @GetMapping("/{id}/delivery-location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        // Access check through getOrderById
        orderService.getOrderById(id, principal.getId(), principal.getRole());
        Map<String, Object> location = deliveryService.getLiveDeliveryLocation(id);
        return ResponseEntity.ok(ApiResponse.success(location));
    }
}
