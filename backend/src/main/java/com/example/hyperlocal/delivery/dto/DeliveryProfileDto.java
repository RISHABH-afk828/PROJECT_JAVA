package com.example.hyperlocal.delivery.dto;

import com.example.hyperlocal.delivery.entity.DeliveryPartnerProfile;

public class DeliveryProfileDto {
    private Long id;
    private Long userId;
    private String vehicleType;
    private String vehicleNumber;
    private Boolean isOnline;
    private Double currentLatitude;
    private Double currentLongitude;
    private Integer totalDeliveries;

    public static DeliveryProfileDto fromEntity(DeliveryPartnerProfile profile) {
        DeliveryProfileDto dto = new DeliveryProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUserId());
        dto.setVehicleType(profile.getVehicleType());
        dto.setVehicleNumber(profile.getVehicleNumber());
        dto.setIsOnline(profile.getIsOnline());
        dto.setCurrentLatitude(profile.getCurrentLatitude());
        dto.setCurrentLongitude(profile.getCurrentLongitude());
        dto.setTotalDeliveries(profile.getTotalDeliveries());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public Integer getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(Integer totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }
}
