package com.example.hyperlocal.order.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    VENDOR_PENDING,
    ACCEPTED,
    PREPARING,
    READY,
    DELIVERY_ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED,
    PAYMENT_FAILED,
    DELIVERY_FAILED,
    REFUNDED
}
