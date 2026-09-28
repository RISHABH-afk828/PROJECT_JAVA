package com.example.hyperlocal.payment.dto;

import java.math.BigDecimal;

public class PaymentOrderResponse {
    private Long orderId;
    private String orderNumber;
    private String providerOrderId;
    private Long amountInPaise;
    private BigDecimal amountInRupees;
    private String currency;
    private String keyId;
    private String provider;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public Long getAmountInPaise() {
        return amountInPaise;
    }

    public void setAmountInPaise(Long amountInPaise) {
        this.amountInPaise = amountInPaise;
    }

    public BigDecimal getAmountInRupees() {
        return amountInRupees;
    }

    public void setAmountInRupees(BigDecimal amountInRupees) {
        this.amountInRupees = amountInRupees;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
