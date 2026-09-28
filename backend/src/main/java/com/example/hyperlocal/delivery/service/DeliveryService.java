package com.example.hyperlocal.delivery.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.delivery.dto.DeliveryLocationUpdateRequest;
import com.example.hyperlocal.delivery.dto.DeliveryProfileDto;
import com.example.hyperlocal.delivery.dto.DeliveryStatusUpdateRequest;
import com.example.hyperlocal.delivery.entity.DeliveryPartnerProfile;
import com.example.hyperlocal.delivery.repository.DeliveryPartnerRepository;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.entity.OrderStatusHistory;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.order.repository.OrderStatusHistoryRepository;
import com.example.hyperlocal.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderService orderService;

    public DeliveryService(
            DeliveryPartnerRepository deliveryPartnerRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            OrderService orderService) {
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.orderService = orderService;
    }

    @Transactional
    public DeliveryProfileDto getOrCreateProfile(Long userId) {
        DeliveryPartnerProfile profile = deliveryPartnerRepository.findByUserId(userId)
                .orElseGet(() -> deliveryPartnerRepository.save(new DeliveryPartnerProfile(userId)));
        return DeliveryProfileDto.fromEntity(profile);
    }

    @Transactional
    public DeliveryProfileDto updateOnlineStatus(Long userId, DeliveryStatusUpdateRequest req) {
        DeliveryPartnerProfile profile = deliveryPartnerRepository.findByUserId(userId)
                .orElseGet(() -> new DeliveryPartnerProfile(userId));

        profile.setIsOnline(req.getIsOnline());
        DeliveryPartnerProfile saved = deliveryPartnerRepository.save(profile);
        return DeliveryProfileDto.fromEntity(saved);
    }

    @Transactional
    public DeliveryProfileDto updateLocation(Long userId, DeliveryLocationUpdateRequest req) {
        DeliveryPartnerProfile profile = deliveryPartnerRepository.findByUserId(userId)
                .orElseGet(() -> new DeliveryPartnerProfile(userId));

        profile.setCurrentLatitude(req.getLatitude());
        profile.setCurrentLongitude(req.getLongitude());
        profile.setLastLocationUpdate(Instant.now());
        DeliveryPartnerProfile saved = deliveryPartnerRepository.save(profile);
        return DeliveryProfileDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAvailableOrders(Long userId) {
        DeliveryPartnerProfile profile = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(profile.getIsOnline())) {
            throw new ApiException("DELIVERY_PARTNER_OFFLINE", "You must be online to view available delivery tasks", HttpStatus.BAD_REQUEST);
        }

        // Return all orders ready for pickup with no assigned partner
        List<Order> readyOrders = orderRepository.findByOrderStatus(OrderStatus.READY).stream()
                .filter(o -> o.getDeliveryPartnerId() == null)
                .collect(Collectors.toList());

        return readyOrders.stream()
                .map(orderService::buildOrderDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getMyOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<OrderDto> dtos = orders.getContent().stream()
                .map(orderService::buildOrderDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(page, size), orders.getTotalElements());
    }

    @Transactional
    public OrderDto acceptOrder(Long userId, Long orderId) {
        DeliveryPartnerProfile profile = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(profile.getIsOnline())) {
            throw new ApiException("DELIVERY_PARTNER_OFFLINE", "You must be online to accept delivery tasks", HttpStatus.BAD_REQUEST);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.READY) {
            throw new ApiException("INVALID_ORDER_STATE", "Order is not ready for pickup (Current status: " + order.getOrderStatus() + ")", HttpStatus.BAD_REQUEST);
        }

        if (order.getDeliveryPartnerId() != null) {
            throw new ApiException("ORDER_ALREADY_ASSIGNED", "Order has already been assigned to another delivery partner", HttpStatus.CONFLICT);
        }

        order.setDeliveryPartnerId(userId);
        order.setOrderStatus(OrderStatus.DELIVERY_ASSIGNED);
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.READY,
                OrderStatus.DELIVERY_ASSIGNED,
                userId,
                "Delivery partner assigned and is heading to the store."
        );
        statusHistoryRepository.save(history);
        log.info("Delivery partner {} assigned to order {}", userId, order.getId());

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto confirmPickup(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validatePartnerAssignment(userId, order);

        if (order.getOrderStatus() != OrderStatus.DELIVERY_ASSIGNED) {
            throw new ApiException("INVALID_ORDER_STATE", "Order must be in DELIVERY_ASSIGNED state before pickup", HttpStatus.BAD_REQUEST);
        }

        order.setOrderStatus(OrderStatus.PICKED_UP);
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.DELIVERY_ASSIGNED,
                OrderStatus.PICKED_UP,
                userId,
                "Delivery partner picked up the package from the store."
        );
        statusHistoryRepository.save(history);

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto startOutForDelivery(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validatePartnerAssignment(userId, order);

        if (order.getOrderStatus() != OrderStatus.PICKED_UP) {
            throw new ApiException("INVALID_ORDER_STATE", "Order must be in PICKED_UP state before out for delivery", HttpStatus.BAD_REQUEST);
        }

        order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.PICKED_UP,
                OrderStatus.OUT_FOR_DELIVERY,
                userId,
                "Order is out for delivery and heading to your location."
        );
        statusHistoryRepository.save(history);

        return orderService.buildOrderDto(updated);
    }

    @Transactional
    public OrderDto completeDelivery(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        validatePartnerAssignment(userId, order);

        if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new ApiException("INVALID_ORDER_STATE", "Order must be in OUT_FOR_DELIVERY state before completing delivery", HttpStatus.BAD_REQUEST);
        }

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        Order updated = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED,
                userId,
                "Package delivered to customer successfully."
        );
        statusHistoryRepository.save(history);

        // Increment partner delivery counter
        deliveryPartnerRepository.findByUserId(userId).ifPresent(p -> {
            p.setTotalDeliveries(p.getTotalDeliveries() + 1);
            deliveryPartnerRepository.save(p);
        });

        return orderService.buildOrderDto(updated);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLiveDeliveryLocation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        Map<String, Object> loc = new HashMap<>();
        loc.put("orderId", order.getId());
        loc.put("orderStatus", order.getOrderStatus());
        loc.put("deliveryPartnerId", order.getDeliveryPartnerId());

        if (order.getDeliveryPartnerId() != null) {
            deliveryPartnerRepository.findByUserId(order.getDeliveryPartnerId()).ifPresent(p -> {
                loc.put("latitude", p.getCurrentLatitude());
                loc.put("longitude", p.getCurrentLongitude());
                loc.put("lastLocationUpdate", p.getLastLocationUpdate());
                loc.put("vehicleType", p.getVehicleType());
                loc.put("vehicleNumber", p.getVehicleNumber());
            });
        }

        return loc;
    }

    private void validatePartnerAssignment(Long userId, Order order) {
        if (order.getDeliveryPartnerId() == null || !order.getDeliveryPartnerId().equals(userId)) {
            throw new ApiException("ACCESS_DENIED", "Order is not assigned to you", HttpStatus.FORBIDDEN);
        }
    }
}
