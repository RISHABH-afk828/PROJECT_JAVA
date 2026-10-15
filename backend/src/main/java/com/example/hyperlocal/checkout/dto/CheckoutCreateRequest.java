package com.example.hyperlocal.checkout.dto;

import com.example.hyperlocal.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class CheckoutCreateRequest {

    @NotNull(message = "Delivery address ID is required")
    private Long addressId;

    private PaymentMethod paymentMethod = PaymentMethod.RAZORPAY;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : PaymentMethod.RAZORPAY;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
