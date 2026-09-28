package com.example.hyperlocal.vendor.dto;

import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;

public class VendorDto {
    private Long id;
    private Long ownerUserId;
    private String storeName;
    private String description;
    private String phone;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double deliveryRadiusKm;
    private VendorStatus status;
    private Boolean isOpen;
    private Double distanceKm;
    private String imageUrl;

    public static VendorDto fromEntity(Vendor vendor, Double distanceKm, Boolean isOpen) {
        VendorDto dto = new VendorDto();
        dto.setId(vendor.getId());
        dto.setOwnerUserId(vendor.getOwnerUserId());
        dto.setStoreName(vendor.getStoreName());
        dto.setDescription(vendor.getDescription());
        dto.setPhone(vendor.getPhone());
        dto.setAddress(vendor.getAddress());
        dto.setLatitude(vendor.getLatitude());
        dto.setLongitude(vendor.getLongitude());
        dto.setDeliveryRadiusKm(vendor.getDeliveryRadiusKm());
        dto.setStatus(vendor.getStatus());
        dto.setDistanceKm(distanceKm);
        dto.setIsOpen(isOpen);
        dto.setImageUrl(vendor.getImageUrl());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

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

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
