package com.example.hyperlocal.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class VendorRegistrationRequest {

    @NotBlank(message = "Store name is required")
    private String storeName;

    private String description;

    @NotBlank(message = "Contact phone is required")
    private String phone;

    @NotBlank(message = "Store address is required")
    private String address;

    @NotNull(message = "Store latitude is required")
    private Double latitude;

    @NotNull(message = "Store longitude is required")
    private Double longitude;

    @NotNull(message = "Delivery radius is required")
    @Positive(message = "Delivery radius must be greater than zero")
    private Double deliveryRadiusKm = 5.0;

    private String imageUrl;

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    public void setDeliveryRadiusKm(Double deliveryRadiusKm) {
        this.deliveryRadiusKm = deliveryRadiusKm;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
