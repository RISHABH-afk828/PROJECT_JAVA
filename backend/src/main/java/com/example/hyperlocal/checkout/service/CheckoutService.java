package com.example.hyperlocal.checkout.service;

import com.example.hyperlocal.address.entity.Address;
import com.example.hyperlocal.address.repository.AddressRepository;
import com.example.hyperlocal.cart.entity.Cart;
import com.example.hyperlocal.cart.entity.CartItem;
import com.example.hyperlocal.cart.repository.CartItemRepository;
import com.example.hyperlocal.cart.repository.CartRepository;
import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteDto;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteRequest;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.common.util.HaversineUtil;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.dto.OrderItemDto;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderItem;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.entity.OrderStatusHistory;
import com.example.hyperlocal.order.repository.OrderItemRepository;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.order.repository.OrderStatusHistoryRepository;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import com.example.hyperlocal.product.entity.Product;
import com.example.hyperlocal.product.repository.ProductRepository;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import com.example.hyperlocal.vendor.service.VendorService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final VendorService vendorService;
    private final Random random = new SecureRandom();

    public CheckoutService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            AddressRepository addressRepository,
            VendorRepository vendorRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            VendorService vendorService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.vendorRepository = vendorRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.vendorService = vendorService;
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteDto calculateQuote(Long userId, CheckoutQuoteRequest req) {
        CheckoutContext ctx = validateAndPrepareContext(userId, req.getAddressId());

        CheckoutQuoteDto quote = new CheckoutQuoteDto();
        quote.setSubtotal(ctx.subtotal);
        quote.setDeliveryFee(ctx.deliveryFee);
        quote.setTax(ctx.tax);
        quote.setDiscount(ctx.discount);
        quote.setTotal(ctx.total);
        quote.setCurrency("INR");
        quote.setDistanceKm(ctx.distanceKm);
        quote.setDeliveryAddressFormatted(ctx.address.toFormattedString());
        quote.setVendorName(ctx.vendor.getStoreName());

        return quote;
    }

    @Transactional
    public OrderDto createOrder(Long userId, CheckoutCreateRequest req) {
        CheckoutContext ctx = validateAndPrepareContext(userId, req.getAddressId());

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomerId(userId);
        order.setVendorId(ctx.vendor.getId());
        order.setAddressSnapshot(ctx.address.toFormattedString());
        order.setSubtotal(ctx.subtotal);
        order.setDeliveryFee(ctx.deliveryFee);
        order.setTax(ctx.tax);
        order.setDiscount(ctx.discount);
        order.setTotal(ctx.total);
        order.setCurrency("INR");
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.CREATED);

        Order savedOrder = orderRepository.save(order);

        List<OrderItemDto> itemDtos = new ArrayList<>();
        for (CartItem ci : ctx.items) {
            Product prod = ctx.productMap.get(ci.getProductId());
            BigDecimal lineTotal = prod.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem(
                    savedOrder.getId(),
                    prod.getId(),
                    prod.getName(),
                    prod.getUnit(),
                    ci.getQuantity(),
                    prod.getPrice(),
                    lineTotal
            );
            OrderItem savedItem = orderItemRepository.save(orderItem);
            itemDtos.add(OrderItemDto.fromEntity(savedItem));
        }

        // Add history record
        OrderStatusHistory history = new OrderStatusHistory(
                savedOrder.getId(),
                null,
                OrderStatus.PAYMENT_PENDING,
                userId,
                "Order created, pending payment confirmation"
        );
        orderStatusHistoryRepository.save(history);

        // Clear user's cart
        cartItemRepository.deleteByCartId(ctx.cart.getId());
        ctx.cart.setVendorId(null);
        cartRepository.save(ctx.cart);

        return OrderDto.fromEntity(savedOrder, ctx.vendor.getStoreName(), itemDtos);
    }

    private CheckoutContext validateAndPrepareContext(Long userId, Long addressId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("CART_EMPTY", "Your cart is empty", HttpStatus.BAD_REQUEST));

        if (cart.getVendorId() == null) {
            throw new ApiException("CART_EMPTY", "Your cart is empty", HttpStatus.BAD_REQUEST);
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApiException("CART_EMPTY", "Your cart is empty", HttpStatus.BAD_REQUEST);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Delivery address not found", HttpStatus.NOT_FOUND));

        if (!address.getUserId().equals(userId)) {
            throw new ApiException("ACCESS_DENIED", "Delivery address does not belong to you", HttpStatus.FORBIDDEN);
        }

        Vendor vendor = vendorRepository.findById(cart.getVendorId())
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new ApiException("VENDOR_INACTIVE", "Store is currently inactive and cannot accept orders", HttpStatus.BAD_REQUEST);
        }

        if (!vendorService.calculateIsOpen(vendor)) {
            throw new ApiException("STORE_CLOSED", "Store is currently closed for orders", HttpStatus.BAD_REQUEST);
        }

        // Distance and serviceability
        double distanceKm = HaversineUtil.distance(
                vendor.getLatitude(), vendor.getLongitude(),
                address.getLatitude(), address.getLongitude()
        );

        if (distanceKm > vendor.getDeliveryRadiusKm()) {
            throw new ApiException("LOCATION_NOT_SERVICEABLE",
                    "Delivery address is beyond store's delivery radius of " + vendor.getDeliveryRadiusKm() + " km (Current distance: " + distanceKm + " km)",
                    HttpStatus.BAD_REQUEST,
                    Map.of("distanceKm", distanceKm, "maxRadiusKm", vendor.getDeliveryRadiusKm()));
        }

        // Product & stock validation
        List<Long> productIds = items.stream().map(CartItem::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null || !product.getActive()) {
                throw new ApiException("PRODUCT_UNAVAILABLE",
                        "One or more products in your cart are no longer available",
                        HttpStatus.BAD_REQUEST);
            }

            Inventory inv = inventoryMap.get(item.getProductId());
            int available = inv != null ? inv.getAvailableQuantity() : 0;
            if (item.getQuantity() > available) {
                throw new ApiException("INSUFFICIENT_STOCK",
                        "Product '" + product.getName() + "' does not have enough stock",
                        HttpStatus.BAD_REQUEST,
                        Map.of("productName", product.getName(), "available", available, "requested", item.getQuantity()));
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        // Server-authoritative calculations
        // Base delivery fee: ₹30.00 for up to 2 km, + ₹10/km beyond 2 km
        double extraDistance = Math.max(0.0, distanceKm - 2.0);
        BigDecimal deliveryFee = BigDecimal.valueOf(30.00)
                .add(BigDecimal.valueOf(extraDistance * 10.0))
                .setScale(2, RoundingMode.HALF_UP);

        // Tax: 5% of subtotal
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(deliveryFee).add(tax).subtract(discount).setScale(2, RoundingMode.HALF_UP);

        CheckoutContext ctx = new CheckoutContext();
        ctx.cart = cart;
        ctx.items = items;
        ctx.address = address;
        ctx.vendor = vendor;
        ctx.productMap = productMap;
        ctx.distanceKm = distanceKm;
        ctx.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        ctx.deliveryFee = deliveryFee;
        ctx.tax = tax;
        ctx.discount = discount;
        ctx.total = total;

        return ctx;
    }

    private static class CheckoutContext {
        Cart cart;
        List<CartItem> items;
        Address address;
        Vendor vendor;
        Map<Long, Product> productMap;
        double distanceKm;
        BigDecimal subtotal;
        BigDecimal deliveryFee;
        BigDecimal tax;
        BigDecimal discount;
        BigDecimal total;
    }
}
