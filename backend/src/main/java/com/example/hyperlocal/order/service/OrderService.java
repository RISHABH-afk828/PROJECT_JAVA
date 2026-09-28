package com.example.hyperlocal.order.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.dto.OrderItemDto;
import com.example.hyperlocal.order.dto.OrderStatusHistoryDto;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderItem;
import com.example.hyperlocal.order.entity.OrderStatusHistory;
import com.example.hyperlocal.order.repository.OrderItemRepository;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.order.repository.OrderStatusHistoryRepository;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final VendorRepository vendorRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            VendorRepository vendorRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getCustomerOrders(Long customerId, int page, int size) {
        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
        List<OrderDto> dtos = orders.getContent().stream()
                .map(this::buildOrderDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(page, size), orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId, Long currentUserId, Role currentUserRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOrderAccess(order, currentUserId, currentUserRole);
        return buildOrderDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByNumber(String orderNumber, Long currentUserId, Role currentUserRole) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOrderAccess(order, currentUserId, currentUserRole);
        return buildOrderDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDto> getOrderStatusHistory(Long orderId, Long currentUserId, Role currentUserRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validateOrderAccess(order, currentUserId, currentUserRole);
        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        return history.stream().map(OrderStatusHistoryDto::fromEntity).collect(Collectors.toList());
    }

    public OrderDto buildOrderDto(Order order) {
        Vendor vendor = vendorRepository.findById(order.getVendorId()).orElse(null);
        String vendorName = vendor != null ? vendor.getStoreName() : "Local Vendor";

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemDto> itemDtos = items.stream().map(OrderItemDto::fromEntity).collect(Collectors.toList());

        return OrderDto.fromEntity(order, vendorName, itemDtos);
    }

    private void validateOrderAccess(Order order, Long currentUserId, Role currentUserRole) {
        if (currentUserRole == Role.ADMIN) {
            return;
        }
        if (order.getCustomerId().equals(currentUserId)) {
            return;
        }
        if (order.getDeliveryPartnerId() != null && order.getDeliveryPartnerId().equals(currentUserId)) {
            return;
        }
        if (currentUserRole == Role.VENDOR) {
            Vendor vendor = vendorRepository.findById(order.getVendorId()).orElse(null);
            if (vendor != null && vendor.getOwnerUserId().equals(currentUserId)) {
                return;
            }
        }
        throw new ApiException("ACCESS_DENIED", "You do not have permission to view this order", HttpStatus.FORBIDDEN);
    }
}
