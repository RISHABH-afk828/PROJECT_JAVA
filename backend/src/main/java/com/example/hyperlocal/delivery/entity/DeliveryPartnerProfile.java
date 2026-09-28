package com.example.hyperlocal.delivery.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_partner_profiles", indexes = {
        @Index(name = "idx_delivery_user", columnList = "user_id", unique = true),
        @Index(name = "idx_delivery_online", columnList = "is_online")
})
public class DeliveryPartnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType = "MOTORCYCLE";

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber = "KA-01-AB-1234";

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "current_latitude")
    private Double currentLatitude = 12.9352;

    @Column(name = "current_longitude")
    private Double currentLongitude = 77.6245;

    @Column(name = "last_location_update")
    private Instant lastLocationUpdate = Instant.now();

    @Column(name = "total_deliveries", nullable = false)
    private Integer totalDeliveries = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public DeliveryPartnerProfile() {
    }

    public DeliveryPartnerProfile(Long userId) {
        this.userId = userId;
        this.isOnline = false;
        this.vehicleType = "MOTORCYCLE";
        this.totalDeliveries = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public Instant getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public void setLastLocationUpdate(Instant lastLocationUpdate) {
        this.lastLocationUpdate = lastLocationUpdate;
    }

    public Integer getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(Integer totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
