package com.example.hyperlocal.vendor.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "store_hours", indexes = {
        @Index(name = "idx_store_hours_vendor", columnList = "vendor_id")
})
public class StoreHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1 = Monday, 7 = Sunday

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private Boolean enabled = true;

    public StoreHours() {
    }

    public StoreHours(Long vendorId, Integer dayOfWeek, LocalTime openTime, LocalTime closeTime, Boolean enabled) {
        this.vendorId = vendorId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.enabled = enabled != null ? enabled : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOpenNow(LocalTime now) {
        if (!Boolean.TRUE.equals(enabled)) return false;
        return !now.isBefore(openTime) && !now.isAfter(closeTime);
    }
}
