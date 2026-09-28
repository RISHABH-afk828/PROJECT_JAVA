package com.example.hyperlocal.vendor.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderItem;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.entity.OrderStatusHistory;
import com.example.hyperlocal.order.repository.OrderItemRepository;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.order.repository.OrderStatusHistoryRepository;
import com.example.hyperlocal.order.service.OrderService;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendorOrderService {

    private static final Logger log = LoggerFactory.getLogger(VendorOrderService.class);

    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final OrderService orderService;

    public VendorOrderService(
            VendorRepository vendorRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            OrderService orderService) {
        this.vendorRepository = vendorRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getVendorOrders(Long ownerUserId, OrderStatus status, int page, int size) {
        Vendor vendor = getVendorForOwner(ownerUserId);

        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByVendorIdAndOrderStatusOrderByCreatedAtDesc(
                    vendor.getId(), status, PageRequest.of(page, size));
        } else {
            orders = orderRepository.findByVendorIdOrderByCreatedAtDesc(
                    vendor.getId(), PageRequest.of(page, size));
        }

        List<OrderDto> dtos = orders.getContent().stream()
                .map(orderService::buildOrderDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, PageRequest.of(page, size), orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderDto getVendorOrderDetails(Long ownerUserId, Long orderId) {
        Vendor vendor = getVendorForOwner(ownerUserId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (!order.getVendorId().equals(vendor.getId())) {
            throw new ApiException("ACCESS_DENIED", "Order does not belong to your store", HttpStatus.FORBIDDEN);
        }

        return orderService.buildOrderDto(order);
    }

    @Transactional
    public OrderDto acceptOrder(Long ownerUserId, Long orderId) {
        Vendor vendor = getVendorForOwner(ownerUserId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOwnership(vendor, order);

        if (order.getOrderStatus() != OrderStatus.VENDOR_PENDING && order.getOrderStatus() != OrderStatus.PAID) {
            throw new ApiException("INVALID_ORDER_STATE",
                    "Cannot accept order in status: " + order.getOrderStatus(), HttpStatus.BAD_REQUEST);
        }

        OrderStatus fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(Instant.now());
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                fromStatus,
                OrderStatus.ACCEPTED,
                ownerUserId,
                "Store accepted the order and started preparation."
        );
        statusHistoryRepository.save(history);
        log.info("Vendor {} accepted order {}", vendor.getId(), order.getId());

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto startPreparingOrder(Long ownerUserId, Long orderId) {
        Vendor vendor = getVendorForOwner(ownerUserId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOwnership(vendor, order);

        if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new ApiException("INVALID_ORDER_STATE",
                    "Cannot start preparing order in status: " + order.getOrderStatus(), HttpStatus.BAD_REQUEST);
        }

        order.setOrderStatus(OrderStatus.PREPARING);
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.ACCEPTED,
                OrderStatus.PREPARING,
                ownerUserId,
                "Order is currently being packed and prepared."
        );
        statusHistoryRepository.save(history);

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto markOrderReady(Long ownerUserId, Long orderId) {
        Vendor vendor = getVendorForOwner(ownerUserId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOwnership(vendor, order);

        if (order.getOrderStatus() != OrderStatus.PREPARING && order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new ApiException("INVALID_ORDER_STATE",
                    "Cannot mark order ready in status: " + order.getOrderStatus(), HttpStatus.BAD_REQUEST);
        }

        OrderStatus fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.READY);
        order.setPreparedAt(Instant.now());
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                fromStatus,
                OrderStatus.READY,
                ownerUserId,
                "Order is packed, sealed, and ready for delivery partner pickup."
        );
        statusHistoryRepository.save(history);
        log.info("Vendor {} marked order {} as READY", vendor.getId(), order.getId());

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto rejectOrder(Long ownerUserId, Long orderId, String reason) {
        Vendor vendor = getVendorForOwner(ownerUserId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOwnership(vendor, order);

        if (order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY ||
                order.getOrderStatus() == OrderStatus.PICKED_UP ||
                order.getOrderStatus() == OrderStatus.REJECTED ||
                order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ApiException("INVALID_ORDER_STATE",
                    "Cannot reject order in status: " + order.getOrderStatus(), HttpStatus.BAD_REQUEST);
        }

        OrderStatus fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.REJECTED);
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                fromStatus,
                OrderStatus.REJECTED,
                ownerUserId,
                "Store rejected order: " + (reason != null ? reason.trim() : "Unable to fulfill order at this time")
        );
        statusHistoryRepository.save(history);

        // RESTORE INVENTORY
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Inventory inv = inventoryRepository.findWithLockByProductId(item.getProductId()).orElse(null);
            if (inv != null) {
                int restored = inv.getAvailableQuantity() + item.getQuantity();
                inv.setAvailableQuantity(restored);
                inventoryRepository.save(inv);

                InventoryMovement movement = new InventoryMovement(
                        item.getProductId(),
                        item.getQuantity(),
                        restored,
                        "CANCEL_RESTORE",
                        "ORDER_REJECTED",
                        String.valueOf(order.getId()),
                        ownerUserId
                );
                movementRepository.save(movement);
                log.info("Restored {} units of product {} due to vendor rejection", item.getQuantity(), item.getProductId());
            }
        }

        return orderService.buildOrderDto(updated);
    }

    private Vendor getVendorForOwner(Long ownerUserId) {
        return vendorRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "No store registered for this account", HttpStatus.NOT_FOUND));
    }

    private void validateOwnership(Vendor vendor, Order order) {
        if (!order.getVendorId().equals(vendor.getId())) {
            throw new ApiException("ACCESS_DENIED", "Order does not belong to your store", HttpStatus.FORBIDDEN);
        }
    }
}
