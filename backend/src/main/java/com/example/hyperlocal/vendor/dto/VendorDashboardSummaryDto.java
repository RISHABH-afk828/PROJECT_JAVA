package com.example.hyperlocal.vendor.dto;

import java.math.BigDecimal;

public class VendorDashboardSummaryDto {

    private long todayOrders;
    private long yesterdayOrders;
    private long pendingOrders;
    private long awaitingPaymentOrders;
    private BigDecimal todaySales;
    private long completedOrdersToday;
    private Double orderGrowthPercent;
    private String orderGrowthLabel;

    public VendorDashboardSummaryDto() {
    }

    public VendorDashboardSummaryDto(
            long todayOrders,
            long yesterdayOrders,
            long pendingOrders,
            long awaitingPaymentOrders,
            BigDecimal todaySales,
            long completedOrdersToday,
            Double orderGrowthPercent,
            String orderGrowthLabel) {
        this.todayOrders = todayOrders;
        this.yesterdayOrders = yesterdayOrders;
        this.pendingOrders = pendingOrders;
        this.awaitingPaymentOrders = awaitingPaymentOrders;
        this.todaySales = todaySales;
        this.completedOrdersToday = completedOrdersToday;
        this.orderGrowthPercent = orderGrowthPercent;
        this.orderGrowthLabel = orderGrowthLabel;
    }

    public VendorDashboardSummaryDto(
            long todayOrders,
            long yesterdayOrders,
            long pendingOrders,
            BigDecimal todaySales,
            long completedOrdersToday,
            Double orderGrowthPercent,
            String orderGrowthLabel) {
        this(todayOrders, yesterdayOrders, pendingOrders, 0L, todaySales, completedOrdersToday, orderGrowthPercent, orderGrowthLabel);
    }

    public long getTodayOrders() {
        return todayOrders;
    }

    public void setTodayOrders(long todayOrders) {
        this.todayOrders = todayOrders;
    }

    public long getYesterdayOrders() {
        return yesterdayOrders;
    }

    public void setYesterdayOrders(long yesterdayOrders) {
        this.yesterdayOrders = yesterdayOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getAwaitingPaymentOrders() {
        return awaitingPaymentOrders;
    }

    public void setAwaitingPaymentOrders(long awaitingPaymentOrders) {
        this.awaitingPaymentOrders = awaitingPaymentOrders;
    }

    public BigDecimal getTodaySales() {
        return todaySales;
    }

    public void setTodaySales(BigDecimal todaySales) {
        this.todaySales = todaySales;
    }

    public long getCompletedOrdersToday() {
        return completedOrdersToday;
    }

    public void setCompletedOrdersToday(long completedOrdersToday) {
        this.completedOrdersToday = completedOrdersToday;
    }

    public Double getOrderGrowthPercent() {
        return orderGrowthPercent;
    }

    public void setOrderGrowthPercent(Double orderGrowthPercent) {
        this.orderGrowthPercent = orderGrowthPercent;
    }

    public String getOrderGrowthLabel() {
        return orderGrowthLabel;
    }

    public void setOrderGrowthLabel(String orderGrowthLabel) {
        this.orderGrowthLabel = orderGrowthLabel;
    }
}
