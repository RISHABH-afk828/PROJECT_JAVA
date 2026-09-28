package com.example.hyperlocal.payment.entity;

public enum PaymentStatus {
    CREATED,
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED
}
