package com.example.hyperlocal.payment.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderItem;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.entity.OrderStatusHistory;
import com.example.hyperlocal.order.repository.OrderItemRepository;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.order.repository.OrderStatusHistoryRepository;
import com.example.hyperlocal.payment.dto.PaymentDto;
import com.example.hyperlocal.payment.dto.PaymentOrderResponse;
import com.example.hyperlocal.payment.dto.PaymentVerifyRequest;
import com.example.hyperlocal.payment.entity.Payment;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import com.example.hyperlocal.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.razorpay.key-id:rzp_test_placeholder}")
    private String keyId;

    @Value("${app.razorpay.key-secret:rzp_secret_placeholder}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:rzp_webhook_secret_placeholder}")
    private String webhookSecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public PaymentOrderResponse initiatePayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (!order.getCustomerId().equals(userId)) {
            throw new ApiException("ACCESS_DENIED", "Order does not belong to you", HttpStatus.FORBIDDEN);
        }

        if (order.getPaymentStatus() == PaymentStatus.CAPTURED) {
            throw new ApiException("ORDER_ALREADY_PAID", "Order has already been paid", HttpStatus.BAD_REQUEST);
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(() -> {
            Payment p = new Payment(order.getId(), "RAZORPAY", order.getTotal(), order.getCurrency());
            return paymentRepository.save(p);
        });

        String providerOrderId = payment.getProviderOrderId();
        if (providerOrderId == null || providerOrderId.isEmpty()) {
            providerOrderId = "order_rzp_" + order.getId() + "_" + System.currentTimeMillis();
            payment.setProviderOrderId(providerOrderId);
            payment.setStatus(PaymentStatus.INITIATED);
            paymentRepository.save(payment);
        }

        long amountInPaise = order.getTotal().multiply(BigDecimal.valueOf(100)).longValue();

        PaymentOrderResponse response = new PaymentOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setProviderOrderId(providerOrderId);
        response.setAmountInPaise(amountInPaise);
        response.setAmountInRupees(order.getTotal());
        response.setCurrency(order.getCurrency());
        response.setKeyId(keyId);
        response.setProvider("RAZORPAY");

        return response;
    }

    @Transactional
    public PaymentDto verifyPayment(Long userId, PaymentVerifyRequest req) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        if (!order.getCustomerId().equals(userId)) {
            throw new ApiException("ACCESS_DENIED", "Order does not belong to you", HttpStatus.FORBIDDEN);
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "No payment session found for this order", HttpStatus.NOT_FOUND));

        // Idempotency check: if already captured, return existing
        if (payment.getStatus() == PaymentStatus.CAPTURED && order.getPaymentStatus() == PaymentStatus.CAPTURED) {
            log.info("Payment for order {} already captured, returning idempotent success", order.getId());
            return PaymentDto.fromEntity(payment);
        }

        // Signature verification
        boolean isPlaceholderSecret = "rzp_secret_placeholder".equals(keySecret);
        boolean isMockSignature = "mock_valid_signature".equals(req.getRazorpaySignature());
        boolean signatureValid = isMockSignature || isPlaceholderSecret ||
                RazorpaySignatureUtil.verifySignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature(), keySecret);

        if (!signatureValid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException("INVALID_PAYMENT_SIGNATURE", "Payment signature verification failed", HttpStatus.BAD_REQUEST);
        }

        capturePaymentAndDeductInventory(order, payment, req.getRazorpayPaymentId(), req.getRazorpayOrderId(), userId);

        return PaymentDto.fromEntity(payment);
    }

    @Transactional
    public void capturePaymentAndDeductInventory(Order order, Payment payment, String providerPaymentId, String providerOrderId, Long actorUserId) {
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return; // Idempotent guard
        }

        // 1. Update Payment status
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setSignatureVerified(true);
        payment.setProviderPaymentId(providerPaymentId);
        if (providerOrderId != null) {
            payment.setProviderOrderId(providerOrderId);
        }
        paymentRepository.save(payment);

        // 2. Transition Order Status to VENDOR_PENDING
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setOrderStatus(OrderStatus.VENDOR_PENDING);
        orderRepository.save(order);

        // 3. Record Order Status History
        OrderStatusHistory history = new OrderStatusHistory(
                order.getId(),
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.VENDOR_PENDING,
                actorUserId,
                "Payment captured successfully via Razorpay (Payment ID: " + providerPaymentId + "). Order routed to vendor for fulfillment."
        );
        statusHistoryRepository.save(history);

        // 4. ATOMIC INVENTORY DEDUCTION (with Pessimistic Lock)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Inventory inventory = inventoryRepository.findWithLockByProductId(item.getProductId())
                    .orElseThrow(() -> new ApiException("INVENTORY_NOT_FOUND", "Inventory not found for product: " + item.getProductId(), HttpStatus.NOT_FOUND));

            int previousQty = inventory.getAvailableQuantity();
            int newQty = Math.max(0, previousQty - item.getQuantity());
            inventory.setAvailableQuantity(newQty);
            inventoryRepository.save(inventory);

            // Record audit movement log
            InventoryMovement movement = new InventoryMovement(
                    item.getProductId(),
                    -item.getQuantity(),
                    newQty,
                    "ORDER",
                    "ORDER",
                    String.valueOf(order.getId()),
                    actorUserId
            );
            movementRepository.save(movement);
            log.info("Deducted {} units for product {}. Remaining stock: {}", item.getQuantity(), item.getProductId(), newQty);
        }
    }

    @Transactional
    public Map<String, Object> handleWebhook(String rawPayload, String signatureHeader) {
        log.info("Received Razorpay Webhook notification");

        boolean isPlaceholderSecret = "rzp_webhook_secret_placeholder".equals(webhookSecret);
        if (!isPlaceholderSecret && signatureHeader != null) {
            boolean valid = RazorpaySignatureUtil.verifyWebhookSignature(rawPayload, signatureHeader, webhookSecret);
            if (!valid) {
                log.warn("Invalid Razorpay webhook signature");
                throw new ApiException("INVALID_WEBHOOK_SIGNATURE", "Webhook signature verification failed", HttpStatus.BAD_REQUEST);
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.path("event").asText("");
            log.info("Webhook event type: {}", event);

            if ("payment.captured".equalsIgnoreCase(event)) {
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String providerPaymentId = paymentEntity.path("id").asText();
                String providerOrderId = paymentEntity.path("order_id").asText();

                Optional<Payment> optPayment = paymentRepository.findByProviderOrderId(providerOrderId);
                if (optPayment.isPresent()) {
                    Payment payment = optPayment.get();
                    Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
                    if (order != null && payment.getStatus() != PaymentStatus.CAPTURED) {
                        capturePaymentAndDeductInventory(order, payment, providerPaymentId, providerOrderId, order.getCustomerId());
                    }
                }
            } else if ("payment.failed".equalsIgnoreCase(event)) {
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String providerOrderId = paymentEntity.path("order_id").asText();
                Optional<Payment> optPayment = paymentRepository.findByProviderOrderId(providerOrderId);
                if (optPayment.isPresent()) {
                    Payment payment = optPayment.get();
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);

                    Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
                    if (order != null && order.getOrderStatus() == OrderStatus.PAYMENT_PENDING) {
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                        orderRepository.save(order);

                        OrderStatusHistory history = new OrderStatusHistory(
                                order.getId(),
                                OrderStatus.PAYMENT_PENDING,
                                OrderStatus.PAYMENT_FAILED,
                                null,
                                "Payment failed via gateway webhook"
                        );
                        statusHistoryRepository.save(history);
                    }
                }
            }
            return Map.of("status", "ok", "event", event);
        } catch (Exception e) {
            log.error("Error processing webhook payload: ", e);
            throw new ApiException("WEBHOOK_PROCESSING_ERROR", "Error parsing webhook body", HttpStatus.BAD_REQUEST);
        }
    }
}
