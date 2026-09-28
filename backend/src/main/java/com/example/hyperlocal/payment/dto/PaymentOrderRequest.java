package com.example.hyperlocal.payment.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentOrderRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
