package com.example.hyperlocal.order.dto;

import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.entity.OrderStatusHistory;

import java.time.Instant;

public class OrderStatusHistoryDto {
    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String reason;
    private Instant createdAt;

    public static OrderStatusHistoryDto fromEntity(OrderStatusHistory h) {
        OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
        dto.setId(h.getId());
        dto.setFromStatus(h.getFromStatus());
        dto.setToStatus(h.getToStatus());
        dto.setReason(h.getReason());
        dto.setCreatedAt(h.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(OrderStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
