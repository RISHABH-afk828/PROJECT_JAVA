package com.example.hyperlocal.checkout.dto;

import jakarta.validation.constraints.NotNull;

public class CheckoutQuoteRequest {

    @NotNull(message = "Delivery address ID is required")
    private Long addressId;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
