package com.example.hyperlocal.admin.dto;

import java.math.BigDecimal;

public class AdminDashboardMetricsDto {
    private Long totalVendors;
    private Long activeVendors;
    private Long pendingVendors;
    private Long totalCustomers;
    private Long totalDeliveryPartners;
    private Long totalOrders;
    private BigDecimal totalGmv;
    private Long completedOrders;
    private Long activeOrders;

    public Long getTotalVendors() {
        return totalVendors;
    }

    public void setTotalVendors(Long totalVendors) {
        this.totalVendors = totalVendors;
    }

    public Long getActiveVendors() {
        return activeVendors;
    }

    public void setActiveVendors(Long activeVendors) {
        this.activeVendors = activeVendors;
    }

    public Long getPendingVendors() {
        return pendingVendors;
    }

    public void setPendingVendors(Long pendingVendors) {
        this.pendingVendors = pendingVendors;
    }

    public Long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(Long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public Long getTotalDeliveryPartners() {
        return totalDeliveryPartners;
    }

    public void setTotalDeliveryPartners(Long totalDeliveryPartners) {
        this.totalDeliveryPartners = totalDeliveryPartners;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalGmv() {
        return totalGmv;
    }

    public void setTotalGmv(BigDecimal totalGmv) {
        this.totalGmv = totalGmv;
    }

    public Long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(Long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public Long getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(Long activeOrders) {
        this.activeOrders = activeOrders;
    }
}
